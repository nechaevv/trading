package ru.osfb.trading.cases

import java.io.FileWriter
import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.{EMA, TreeTradeHistory, TrendFactor, Volatility}
import ru.osfb.trading.connectors.{Bitcoincharts, DatabaseHistoryStore}

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
  Await.ready(for(trades <- DatabaseHistoryStore.loadHistory(args(0), from, till)) yield {
    logger.info("History load completed")
    val history = new TreeTradeHistory(trades)
    val writer = new FileWriter("out.txt", false)
    logger.info(s"Running computation from $from till $till")
    for (time <- (fromSec + timeQuantum) to tillSec by timeQuantum) {
      val price = EMA(history, time, timeQuantum)
      val volatility = Volatility(history, time, timeQuantum)
      val trendFactor = TrendFactor(history, time - trendTimeFrame, time, timeQuantum)
      logger.info(s"Time $time, price $price, trendFactor: $trendFactor")
      writer.write(s"$time $price $volatility $trendFactor\n")
    }
    logger.info("Completed")
    writer.close()
  }, 1.hour)

}
