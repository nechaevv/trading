package ru.osfb.trading.cases

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.ArrayTradeHistory
import ru.osfb.trading.connectors.{BfxData, CsvHistoryStore}
import ru.osfb.trading.strategies.TrendStrategy

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by sgl on 03.05.16.
  */
object TrendStrategyBackTest extends App with LazyLogging {
  val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
  def toInstant(s: String) = Instant.from(LocalDateTime.from(dateFormatter.parse(s)).atZone(ZoneId.systemDefault()))

  val from = toInstant(args(1))
  val till = toInstant(args(2))
  val fromSec = from.toEpochMilli / 1000
  val tillSec = till.toEpochMilli / 1000
  val timeFrame = args(3).toLong
  val avgTimeFactor = args(4).toLong
  val openFactor = args(5).toDouble
  val closeFactor = args(6).toDouble

  val strategy = new TrendStrategy(timeFrame, avgTimeFactor, openFactor, closeFactor)
  val fetchTimeStart = from minusSeconds (timeFrame * (1.0/avgTimeFactor + 1.0)).toLong
  val trades = Await.result(CsvHistoryStore.loadHistory(args(0), fetchTimeStart, till), 1.hour)
  //val trades = BfxData.loadVwapData("BTCUSD", fetchTimeStart.toEpochMilli / 1000, till.toEpochMilli / 1000)
  implicit val history = new ArrayTradeHistory(trades)

  val runner = new StrategyBackTest

  logger.info(s"Running backtest from $from till $till")
  val stat = runner.run(strategy, fromSec, tillSec, timeFrame / (6 * avgTimeFactor))

  logger.info("Backtest completed")
  logger.info(s"Succeded: ${stat.succeeded.count} trades with ${stat.succeeded.profit * 100}% profit, avg time: ${stat.succeeded.time / (86400 * stat.succeeded.count)} days")
  logger.info(s"Failed: ${stat.failed.count} trades with ${stat.failed.profit * 100}% loss, avg time: ${stat.failed.time / (86400 * stat.failed.count)} days")
  logger.info(s"Total: ${stat.succeeded.count + stat.failed.count} trades with ${(stat.succeeded.profit + stat.failed.profit) * 100}% profit" +
    s", avg time: ${(stat.succeeded.time + stat.failed.time) / (86400 * (stat.succeeded.count + stat.failed.count))} days")

}
