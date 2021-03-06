package ru.osfb.trading.cases

import java.io.FileWriter
import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.ArrayTradeHistory
import ru.osfb.trading.connectors.{BfxData, CsvHistoryStore, Finam}
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
  val timeStep = args(3).toLong
  val timeFrame = args(4).toLong
  val localTimeFactor = args(5).toLong
  val localTrendFactor = args(6).toDouble
  val openFactor = args(7).toDouble
  val closeFactor = args(8).toDouble
  val orderVolFactor = args(9).toDouble
  val orderExecTimeFactor = args(10).toLong

  val fromSec = from.toEpochMilli / 1000
  val tillSec = till.toEpochMilli / 1000

  val fetchTimeStart = from minusSeconds 5*timeFrame
  val trades =
  //Await.result(CsvHistoryStore.loadHistory(args(0), fetchTimeStart, till), 1.hour)
  //BfxData.loadVwapData("BTCUSD", fetchTimeStart.toEpochMilli / 1000, till.toEpochMilli / 1000)
    Finam.loadCsvTrades(args(0))
  implicit val history = new ArrayTradeHistory(trades)

  val runner = new StrategyBackTest


  logger.info(s"Running optimize from $from till $till")
  //Time frame
  //def strategyFactory(param: Long) = new TrendStrategy(param, localTimeFactor, localTrendFactor, openFactor, closeFactor, orderVolFactor, orderExecTimeFactor)
  //val stat = runner.optimize(strategyFactory, 50000L to 1000000L by 1000L, fromSec, tillSec, timeStep)
  //Local time factor
  //def strategyFactory(param: Long) = new TrendStrategy(timeFrame, param, localTrendFactor, openFactor, closeFactor, orderVolFactor, orderExecTimeFactor)
  //val stat = runner.optimize(strategyFactory, 2L to 300L by 1L, fromSec, tillSec, timeStep)
  //Local trend factor
  //def strategyFactory(param: Double) = new TrendStrategy(timeFrame, localTimeFactor, param, openFactor, closeFactor, orderVolFactor, orderExecTimeFactor)
  //val stat = runner.optimize(strategyFactory, -0.5 to 0.5 by 0.002, fromSec, tillSec, timeStep)
  //Open factor
  //def strategyFactory(param: Double) = new TrendStrategy(timeFrame, localTimeFactor, localTrendFactor, param, closeFactor, orderVolFactor, orderExecTimeFactor)
  //val stat = runner.optimize(strategyFactory, -0.2 to 0.9 by 0.002, fromSec, tillSec, timeStep)
  //Close factor
  //def strategyFactory(param: Double) = new TrendStrategy(timeFrame, localTimeFactor, localTrendFactor, openFactor, param, orderVolFactor, orderExecTimeFactor)
  //val stat = runner.optimize(strategyFactory, -0.9 to 0.9 by 0.001, fromSec, tillSec, timeStep)
  //Order volatility factor
  def strategyFactory(param: Double) = new TrendStrategy(timeFrame, localTimeFactor, localTrendFactor, openFactor, closeFactor, param, orderExecTimeFactor)
  val stat = runner.optimize(strategyFactory, 0.0 to 5.0 by 0.01, fromSec, tillSec, timeStep)
  //Order execution time factor
  //def strategyFactory(param: Double) = new TrendStrategy(timeFrame, localTimeFactor, localTrendFactor, openFactor, closeFactor, orderVolFactor, param)
  //val stat = runner.optimize(strategyFactory, 0.2 to 5.0 by 0.1, fromSec, tillSec, timeStep)

  //val zeroParam = 0L
  val zeroParam = 0.0

  val (maxParam, maxProfit) = stat.foldLeft((zeroParam, 0.0))((acc, v) => {
    val (_, maxProfit) = acc
    val (param, stat) = v
    val profit = stat.succeeded.profit + stat.failed.profit
    if (profit > maxProfit) (param, profit) else acc
  })
  logger.info(s"Top profit: ${maxProfit*100}% on $maxParam")

  val fw = new FileWriter(s"optimize/${Instant.now().toString.replace(":","-")}.csv")
  fw.write(stat.foldLeft("")((res, paramStat) => paramStat match {
    case (param, stat) => res + (param.toString + ";" + (stat.succeeded.profit + stat.failed.profit).toString + ";" + (stat.succeeded.count + stat.failed.count).toString).replace(".",",") + "\n"
  }))
  fw.close()

}
