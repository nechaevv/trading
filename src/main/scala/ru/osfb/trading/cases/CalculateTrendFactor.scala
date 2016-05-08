package ru.osfb.trading.cases

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

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

  val time = toInstant(args(1))
  val timeFrame = args(2).toLong

  logger.info("Fetching trade history")
  val fetchTimeStart = time minus java.time.Duration.ofSeconds(timeFrame * 5)
  val trades = Await.result(CsvHistoryStore.loadHistory(args(0), fetchTimeStart, time), 1.hour)
  //val trades = BfxData.loadVwapData("BTCUSD", fetchTimeStart.toEpochMilli / 1000, till.toEpochMilli / 1000)
  //val trades = Finam.loadCsvTrades(args(0))
  logger.info("History load completed")
  implicit val history = new ArrayTradeHistory(trades)
  val trendFactor = CovarianceTrendFactor(time.getEpochSecond, timeFrame)

  logger.info(s"Trend factor at $time = $trendFactor")

}

