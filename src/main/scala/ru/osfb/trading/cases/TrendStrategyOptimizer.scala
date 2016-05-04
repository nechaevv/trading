package ru.osfb.trading.cases

import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.ArrayTradeHistory
import ru.osfb.trading.connectors.{BfxData, CsvHistoryStore}
import ru.osfb.trading.strategies.TrendStrategy

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by sgl on 04.05.16.
  */
object TrendStrategyOptimizer extends App with LazyLogging {
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

  val fetchTimeStart = from minusSeconds (timeFrame * (1.0/avgTimeFactor + 1.0)).toLong
  //val trades = Await.result(CsvHistoryStore.loadHistory(args(0), fetchTimeStart, till), 1.hour)
  val trades = BfxData.loadVwapData("BTCUSD", fetchTimeStart.toEpochMilli / 1000, till.toEpochMilli / 1000)
  implicit val history = new ArrayTradeHistory(trades)

  val runner = new StrategyBackTest


  logger.info(s"Running backtest from $from till $till")
  //Time frame
  def strategyFactory(param: Long) = new TrendStrategy(param, avgTimeFactor, openFactor, closeFactor)
  val stat = runner.optimize(strategyFactory, 50000L to 1000000L by 5000L, fromSec, tillSec, 3600, timeFrame / avgTimeFactor)
  //Avg time factor
  //def strategyFactory(param: Long) = new TrendStrategy(timeFrame, param, openFactor, closeFactor)
  //val stat = runner.optimize(strategyFactory, 10L to 200L by 1L, fromSec, tillSec, 3600)
  //Open factor
  //def strategyFactory(param: Double) = new TrendStrategy(timeFrame, avgTimeFactor, param, closeFactor)
  //val stat = runner.optimize(strategyFactory, 0.5 to 5.0 by 0.025, fromSec, tillSec, 3600)
  //Close factor
  //def strategyFactory(param: Double) = new TrendStrategy(timeFrame, avgTimeFactor, openFactor, param)
  //val stat = runner.optimize(strategyFactory, -2.0 to 2.0 by 0.025, fromSec, tillSec, 3600)



  val (maxParam, maxProfit) = stat.foldLeft((0L, 0.0))((acc, v) => {
    val (_, maxProfit) = acc
    val (_, profit) = v
    if (profit > maxProfit) v else acc
  })

  logger.info(s"Top profit: ${maxProfit*100}% on $maxParam")


}
