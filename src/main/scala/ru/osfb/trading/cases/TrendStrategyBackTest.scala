package ru.osfb.trading.cases

import java.io.FileWriter
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.ArrayTradeHistory
import ru.osfb.trading.connectors.{BfxData, CsvHistoryStore, Finam}
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

  val strategy = new TrendStrategy(
    timeFrame,
    localTimeFactor,
    localTrendFactor,
    openFactor,
    closeFactor,
    orderVolFactor,
    orderExecTimeFactor
  )
  val fetchTimeStart = from minusSeconds 5*timeFrame
  //val trades = Await.result(CsvHistoryStore.loadHistory(args(0), fetchTimeStart, till), 1.hour)
  //val trades = BfxData.loadVwapData("BTCUSD", fetchTimeStart.toEpochMilli / 1000, till.toEpochMilli / 1000)
  val trades = Finam.loadCsvTrades(args(0))
  implicit val history = new ArrayTradeHistory(trades)

  val runner = new StrategyBackTest
  val systime = System.currentTimeMillis()

  logger.info(s"Running backtest from $from till $till")
  val stat = runner.run(strategy, fromSec, tillSec, timeStep)
  logger.info(s"Finished for ${(System.currentTimeMillis() - systime)/1000} sec")

  val info = "Args: " + args.reduce(_ + "," + _) + "\n" +
    (if(stat.succeeded.count > 0) s"Succeded: ${stat.succeeded.count} trades with ${stat.succeeded.profit * 100}% profit, avg time: ${stat.succeeded.time / (86400 * stat.succeeded.count)} days\n" else "") +
    (if(stat.failed.count > 0) s"Failed: ${stat.failed.count} trades with ${stat.failed.profit * 100}% loss, avg time: ${stat.failed.time / (86400 * stat.failed.count)} days\n" else "") +
  s"Total: ${stat.succeeded.count + stat.failed.count} trades with ${(stat.succeeded.profit + stat.failed.profit) * 100}% profit" +
  s", avg time: ${(stat.succeeded.time + stat.failed.time) / (86400 * (stat.succeeded.count + stat.failed.count))} days"

  logger.info(info)

  val fw = new FileWriter("backtest/" + Instant.now().toString.replace(":","-") + ".txt")
  fw.write(info)
  fw.close()

}
