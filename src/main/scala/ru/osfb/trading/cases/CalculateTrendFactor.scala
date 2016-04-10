package ru.osfb.trading.cases

import java.io.FileWriter
import java.time
import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations._
import ru.osfb.trading.connectors.{BfxData, BitcoinchartsServiceComponent, CsvHistoryStore, DatabaseHistoryStore}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by sgl on 09.04.16.
  */
object CalculateTrendFactor extends App with LazyLogging {
  val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
  def toInstant(s: String) = Instant.from(LocalDateTime.from(dateFormatter.parse(s)).atZone(ZoneId.systemDefault()))

  //val trades = Bitcoincharts.loadHistoryFromCsvFile(args(0))
  //val history = new TreeTradeHistory(trades)


  val from = toInstant(args(1))//.toEpochMilli / 1000
  val till = toInstant(args(2))//.toEpochMilli / 1000
  val fromSec = from.toEpochMilli / 1000
  val tillSec = till.toEpochMilli / 1000
  val timeQuantum = args(3).toLong
  val trendTimeFrame = args(4).toLong * timeQuantum
  val openAt = args(5).toDouble
  val closeAt = args(6).toDouble
  val volatilityThreshold = args(7).toDouble
  val txFee = 0.002

  logger.info("Fetching trade history")
  val fetchTimeStart = from minus time.Duration.ofSeconds(trendTimeFrame + timeQuantum*5)
  val trades = Await.result(CsvHistoryStore.loadHistory(args(0), fetchTimeStart, till), 1.hour)
  //val trades = BfxData.loadVwapData("BTCUSD", fetchTimeStart.toEpochMilli / 1000, till.toEpochMilli / 1000)
    logger.info("History load completed")
    val startTime = System.currentTimeMillis()
    //val history = new TreeTradeHistory(trades)
    val history = new ArrayTradeHistory(trades)
    var longPosition: Option[Double] = None
    var shortPosition: Option[Double] = None
    var longStats = TradeStats(0,0,0,0)
    var shortStats = TradeStats(0,0,0,0)
    var fee = 0.0
    logger.info(s"Running computation from $from till $till")
    //val writer = new FileWriter("out.txt", false)
    for (time <- (fromSec + timeQuantum) to tillSec by timeQuantum/4) {
      //val price = EMA(history, time, timeQuantum)
      //val volatility = Volatility(history, time, timeQuantum)
      val TrendProperties(startPrice, endPrice, trendFactor) = TrendFactor(history, time - trendTimeFrame, time, timeQuantum)
      val startVolatilityFactor = Volatility(history, time - trendTimeFrame, timeQuantum) / Math.abs(endPrice - startPrice)
      //logger.info(s"Time $time, price $price, trendFactor: $trendFactor")
      //writer.write(s"$time $price $volatility $trendFactor\n")
      longPosition match {
        case None => if (trendFactor > openAt) { //} && startVolatilityFactor < volatilityThreshold) {
          val price = history.priceAt(time)
          logger.info(s"${asDateTime(time)} Opening long position at $price")
          longPosition = Some(price)
          fee += price * txFee
        }
        case Some(positionPrice) => if (trendFactor <= closeAt) {
          val price = history.priceAt(time)
          logger.info(s"${asDateTime(time)} Closing long position at $price, profit: ${price - positionPrice}")
          longStats = longStats.update(price - positionPrice)
          longPosition = None
          fee += price * txFee
        }
      }
      shortPosition match {
        case None => if (trendFactor < -openAt) { //} && startVolatilityFactor < volatilityThreshold) {
          val price = history.priceAt(time)
          logger.info(s"${asDateTime(time)} Opening short position at $price")
          shortPosition = Some(price)
          fee += price * txFee
        }
        case Some(positionPrice) => if (trendFactor >= -closeAt) {
          val price = history.priceAt(time)
          logger.info(s"${asDateTime(time)} Closing short position at $price, profit: ${positionPrice - price}")
          shortStats = shortStats.update(positionPrice - price)
          shortPosition = None
          fee += price * txFee
        }
      }
    }
    val endTime = System.currentTimeMillis()
    logger.info(s"Completed (${endTime - startTime} ms): " +

      s"profit ${longStats.profit + shortStats.profit + longStats.loss + shortStats.loss}" +
      s"\nLong $longStats\nShort $shortStats\nFee $fee")
    //writer.close()

  case class TradeStats(profit: Double, loss: Double, success: Int, fail: Int) {
    def update(newProfit: Double) = if(newProfit>0) TradeStats(profit + newProfit, loss, success + 1, fail)
    else TradeStats(profit, loss + newProfit, success, fail + 1)
  }

  def asDateTime(ts: Long) = Instant.ofEpochSecond(ts).toString
}

