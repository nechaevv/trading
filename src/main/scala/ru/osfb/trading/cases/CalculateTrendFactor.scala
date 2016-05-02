package ru.osfb.trading.cases

import java.io.FileWriter
import java.time
import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations._
import ru.osfb.trading.connectors._

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


  val from = toInstant(args(1))
  val till = toInstant(args(2))
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
  //val trades = Finam.loadCsvTrades(args(0))
    logger.info("History load completed")
    val startTime = System.currentTimeMillis()
    //val history = new TreeTradeHistory(trades)
    implicit val history = new ArrayTradeHistory(trades)
    var longPosition: Option[Position] = None
    var shortPosition: Option[Position] = None
    var longStats = TradeStats(0,0,0,0,0,0)
    var shortStats = TradeStats(0,0,0,0,0,0)
    var fee = 0.0
    logger.info(s"Running computation from $from till $till")
    //val writer = new FileWriter("out.txt", false)
    for (time <- (fromSec + timeQuantum) to tillSec by timeQuantum/4) {
      //val price = EMA(history, time, timeQuantum)
      //val volatility = Volatility(history, time, timeQuantum)
      val TrendProperties(startPrice, endPrice, trendFactor) = TrendFactor(time - trendTimeFrame, time, timeQuantum)
      val startVolatilityFactor = Volatility(time - trendTimeFrame, timeQuantum) / Math.abs(endPrice - startPrice)
      //logger.info(s"Time $time, price $price, trendFactor: $trendFactor")
      //writer.write(s"$time $price $volatility $trendFactor\n")
      longPosition match {
        case None => if (trendFactor > openAt) { //} && startVolatilityFactor < volatilityThreshold) {
          val price = history.priceAt(time)
          logger.info(s"${asDateTime(time)} Opening long position at $price")
          longPosition = Some(Position(time, price))
          fee += price * txFee
        }
        case Some(position) => if (trendFactor <= closeAt) {
          val price = history.priceAt(time)
          logger.info(s"${asDateTime(time)} Closing long position at $price, profit: ${price - position.price}")
          longStats = longStats.update(price - position.price, time - position.openAt)
          longPosition = None
          fee += price * txFee
        }
      }
      shortPosition match {
        case None => if (trendFactor < -openAt) { //} && startVolatilityFactor < volatilityThreshold) {
          val price = history.priceAt(time)
          logger.info(s"${asDateTime(time)} Opening short position at $price")
          shortPosition = Some(Position(time, price))
          fee += price * txFee
        }
        case Some(position) => if (trendFactor >= -closeAt) {
          val price = history.priceAt(time)
          logger.info(s"${asDateTime(time)} Closing short position at $price, profit: ${position.price - price}")
          shortStats = shortStats.update(position.price - price, time - position.openAt)
          shortPosition = None
          fee += price * txFee
        }
      }
    }
    val endTime = System.currentTimeMillis()
    logger.info(s"Completed (${endTime - startTime} ms): " +

      s"profit ${longStats.profit + shortStats.profit + longStats.loss + shortStats.loss}" +
      s"\nLong\n$longStats\nShort\n$shortStats\nFee $fee")
    //writer.close()

  case class Position(openAt: Long, price: Double)

  case class TradeStats(profit: Double, loss: Double, success: Int, fail: Int, successDuration: Long, failDuration: Long) {
    def update(newProfit: Double, duration: Long) = if(newProfit>0) TradeStats(profit + newProfit, loss, success + 1, fail, successDuration + duration, failDuration)
    else TradeStats(profit, loss + newProfit, success, fail + 1, successDuration, failDuration + duration)

    override def toString: String = s"profit: $profit, loss: $loss, total: ${profit + loss}\n" +
      s"success: $success, avg profit: ${if(success==0) 0 else profit/success}, fail: $fail, avg loss: ${if (fail==0) 0 else loss/fail}, total: ${success + fail}, avg: ${if (success+fail==0) 0 else (profit + loss)/(success + fail)}\n" +
      s"avg success duration: ${if(success==0) 0 else successDuration/(success*3600)} h, avg fail duration: ${if (fail==0) 0 else failDuration/(fail*3600)} h"
  }

  def asDateTime(ts: Long) = Instant.ofEpochSecond(ts).toString
}

