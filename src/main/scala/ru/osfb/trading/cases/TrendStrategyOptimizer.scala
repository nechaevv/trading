package ru.osfb.trading.cases

import java.io.FileWriter
import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.ArrayTradeHistory
import ru.osfb.trading.connectors.{BfxData, CsvHistoryStore}
import ru.osfb.trading.strategies.TrendStrategy

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

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
  val orderVolFactor = args(7).toDouble
  val orderExecTimeFactor = args(8).toDouble

  val fetchTimeStart = from minusSeconds (timeFrame * (1.0/avgTimeFactor + 1.0)).toLong
  val trades = Await.result(CsvHistoryStore.loadHistory(args(0), fetchTimeStart, till), 1.hour)
  //val trades = BfxData.loadVwapData("BTCUSD", fetchTimeStart.toEpochMilli / 1000, till.toEpochMilli / 1000)
  implicit val history = new ArrayTradeHistory(trades)

  val runner = new StrategyBackTest


  logger.info(s"Running backtest from $from till $till")
  //Time frame
  //def strategyFactory(param: Long) = new TrendStrategy(param, avgTimeFactor, openFactor, closeFactor, orderVolFactor, (orderExecTimeFactor * param / avgTimeFactor).toLong)
  //val stat = runner.optimize(strategyFactory, 100000L to 1000000L by 2000L, fromSec, tillSec, 3600)
  //Avg time factor
  //def strategyFactory(param: Long) = new TrendStrategy(timeFrame, param, openFactor, closeFactor, orderVolFactor, (orderExecTimeFactor * timeFrame / param).toLong)
  //val stat = runner.optimize(strategyFactory, 10L to 200L by 1L, fromSec, tillSec, 3600)
  //Open factor
  def strategyFactory(param: Double) = new TrendStrategy(timeFrame, avgTimeFactor, param, closeFactor, orderVolFactor, (orderExecTimeFactor * timeFrame / avgTimeFactor).toLong)
  val stat = runner.optimize(strategyFactory, 0.2 to 5.0 by 0.05, fromSec, tillSec, 3600)
  //Close factor
  //def strategyFactory(param: Double) = new TrendStrategy(timeFrame, avgTimeFactor, openFactor, param, orderVolFactor, (orderExecTimeFactor * timeFrame / avgTimeFactor).toLong)
  //val stat = runner.optimize(strategyFactory, -2.0 to 2.0 by 0.005, fromSec, tillSec, 3600)
  //Order volatility factor
  //def strategyFactory(param: Double) = new TrendStrategy(timeFrame, avgTimeFactor, openFactor, closeFactor, param, (orderExecTimeFactor * timeFrame / avgTimeFactor).toLong)
  //val stat = runner.optimize(strategyFactory, 0.5 to 2.0 by 0.05, fromSec, tillSec, 3600)
  //Order execution time factor
  //def strategyFactory(param: Double) = new TrendStrategy(timeFrame, avgTimeFactor, openFactor, closeFactor, orderVolFactor, (param * timeFrame / avgTimeFactor).toLong)
  //val stat = runner.optimize(strategyFactory, 0.2 to 5.0 by 0.1, fromSec, tillSec, 3600)

  //val zeroParam = 0L
  val zeroParam = 0.0

  val (maxParam, maxProfit) = stat.foldLeft((zeroParam, 0.0))((acc, v) => {
    val (_, maxProfit) = acc
    val (param, stat) = v
    val profit = stat.succeeded.profit + stat.failed.profit
    if (profit > maxProfit) (param, profit) else acc
  })
  logger.info(s"Top profit: ${maxProfit*100}% on $maxParam")

  val fw = new FileWriter("optimize.csv")
  stat.seq.sortBy(_._1) foreach {
    case (param, stat) => fw.write((param.toString + ";" + (stat.succeeded.profit + stat.failed.profit).toString + ";" + (stat.succeeded.count + stat.failed.count).toString).replace(".",",") + "\n")
  }
  fw.close()

}
