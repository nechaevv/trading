package ru.osfb.trading.cases

import java.io.FileWriter
import java.time
import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations._
import ru.osfb.trading.connectors.{Bitcoincharts, CsvHistoryStore, DatabaseHistoryStore}

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
  logger.info("Fetching trade history")
  val fetchTimeStart = from minus time.Duration.ofSeconds(trendTimeFrame + timeQuantum*5)
  Await.ready(for(trades <- CsvHistoryStore.loadHistory(args(0), fetchTimeStart, till)) yield {
    logger.info("History load completed")
    val startTime = System.currentTimeMillis()
    //val history = new TreeTradeHistory(trades)
    val history = new ArrayTradeHistory(trades)
    var longPosition: Option[Double] = None
    var shortPosition: Option[Double] = None
    var longStats = TradeStats(0,0,0,0)
    var shortStats = TradeStats(0,0,0,0)
    logger.info(s"Running computation from $from till $till")
    //val writer = new FileWriter("out.txt", false)
    for (time <- (fromSec + timeQuantum) to tillSec by timeQuantum) {
      val price = EMA(history, time, timeQuantum)
      val volatility = Volatility(history, time, timeQuantum)
      val trendFactor = TrendFactor(history, time - trendTimeFrame, time, timeQuantum)
      //logger.info(s"Time $time, price $price, trendFactor: $trendFactor")
      //writer.write(s"$time $price $volatility $trendFactor\n")
      longPosition match {
        case None => if (trendFactor > 1.6) {
          val price = history.priceAt(time)
          logger.info(s"Opening long position at $price")
          longPosition = Some(price)
        }
        case Some(positionPrice) => if (trendFactor <= -0.3) {
          val price = history.priceAt(time)
          logger.info(s"Closing long position at $price, profit: ${price - positionPrice}")
          longStats = longStats.update(price - positionPrice)
          longPosition = None
        }
      }
      shortPosition match {
        case None => if (trendFactor < -1.6) {
          val price = history.priceAt(time)
          logger.info(s"Opening short position at $price")
          shortPosition = Some(price)
        }
        case Some(positionPrice) => if (trendFactor >= 0.3) {
          val price = history.priceAt(time)
          logger.info(s"Closing short position at $price, profit: ${positionPrice - price}")
          shortStats = shortStats.update(positionPrice - price)
          shortPosition = None
        }
      }
    }
    val endTime = System.currentTimeMillis()
    logger.info(s"Completed (${endTime - startTime} ms): " +
      s"profit ${longStats.profit + shortStats.profit + longStats.loss + shortStats.loss}" +
      s"\nLong $longStats\nShort $shortStats")
    //writer.close()
  }, 1.hour)

  case class TradeStats(profit: Double, loss: Double, success: Int, fail: Int) {
    def update(newProfit: Double) = if(newProfit>0) TradeStats(profit + newProfit, loss, success + 1, fail)
    else TradeStats(profit, loss + newProfit, success, fail + 1)
  }
}

