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
    //val writer = new FileWriter("out.txt", false)
    var longPosition: Option[Double] = None
    var shortPosition: Option[Double] = None
    var profit = 0.0
    var loss = 0.0
    var longSuccess = 0
    var longFail = 0
    var shortSuccess = 0
    var shortFail = 0
    logger.info(s"Running computation from $from till $till")
    for (time <- (fromSec + timeQuantum) to tillSec by timeQuantum) {
      val price = EMA(history, time, timeQuantum)
      val volatility = Volatility(history, time, timeQuantum)
      val trendFactor = TrendFactor(history, time - trendTimeFrame, time, timeQuantum)
      logger.info(s"Time $time, price $price, trendFactor: $trendFactor")
      //writer.write(s"$time $price $volatility $trendFactor\n")
      longPosition match {
        case None => if (trendFactor > 2) {
          val price = history.priceAt(time)
          logger.info(s"Opening long position at $price")
          longPosition = Some(price)
        }
        case Some(positionPrice) => if (trendFactor <= 0.0) {
          val price = history.priceAt(time)
          logger.info(s"Closing long position at $price, profit: ${price - positionPrice}")
          val tradeProfit = price - positionPrice
          if (tradeProfit > 0) {
            profit += tradeProfit
            longSuccess += 1
          } else {
            loss += tradeProfit
            longFail += 1
          }
          longPosition = None
        }
      }
      shortPosition match {
        case None => if (trendFactor < 2) {
          val price = history.priceAt(time)
          logger.info(s"Opening short position at $price")
          shortPosition = Some(price)
        }
        case Some(positionPrice) => if (trendFactor >= 0.0) {
          val price = history.priceAt(time)
          logger.info(s"Closing short position at $price, profit: ${positionPrice - price}")
          val tradeProfit = positionPrice - price
          if (tradeProfit > 0) {
            profit += tradeProfit
            shortSuccess += 1
          } else {
            loss += tradeProfit
            shortFail += 1
          }
          shortPosition = None
        }
      }
    }
    val endTime = System.currentTimeMillis()
    logger.info(s"Completed (${endTime - startTime} ms): profit $profit, loss $loss, total ${profit + loss}" +
      s"\nlong $longSuccess success, $longFail fail, short $shortSuccess success, $shortFail fail")
    //writer.close()
  }, 1.hour)

}
