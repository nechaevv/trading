package ru.osfb.trading.cases

import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.{ArrayTradeHistory, CovarianceTrendFactor, EMA, TrendFactor}
import ru.osfb.trading.connectors.{BfxData, CsvHistoryStore}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by v.a.nechaev on 30.05.2016.
  */
object BalancingStrategyTest extends App with LazyLogging {
  val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
  def toInstant(s: String) = Instant.from(LocalDateTime.from(dateFormatter.parse(s)).atZone(ZoneId.systemDefault()))

  val from = toInstant(args(1))
  val till = toInstant(args(2))
  val timeStep = args(3).toLong
  val defaultTimeFrame = args(4).toLong
  val defaultHysteresis = args(5).toDouble

  val fetchTimeStart = from minusSeconds 86400*365 // minusSeconds 5*timeFrame

  val trades =
  //Await.result(CsvHistoryStore.loadHistory(args(0), fetchTimeStart, till), 1.hour)
  BfxData.loadVwapData("BTCUSD", fetchTimeStart.toEpochMilli / 1000, till.toEpochMilli / 1000)
  //  Finam.loadCsvTrades(args(0))
  implicit val history = new ArrayTradeHistory(trades)

  logger.info(s"Running backtest from $from till $till")

  def run(timeFrame: Long, hysteresis: Double, multiplier: Double) = {
    val startPrice = history.priceAt(from.getEpochSecond) //EMA(from.getEpochSecond, timeStep)
    val (baseBalance, instrumentBalance, turnover) = (from.getEpochSecond to till.getEpochSecond by timeStep)
        .foldLeft((50.0, 50.0 / startPrice, 0.0))((balance, time) => {
          val (baseBalance, instrumentBalance, turnover) = balance
          val tf = CovarianceTrendFactor(time, timeFrame)
          val price = history.priceAt(time) //EMA(time, timeStep)
          val totalBalance = instrumentBalance * price + baseBalance
          val currentRatio = (instrumentBalance * price - baseBalance) / totalBalance
          val diff = tf*multiplier - currentRatio
          if (Math.abs(diff) > hysteresis && currentRatio > -0.9 && currentRatio < 0.9) {
            val adjustedDiff = diff match {
              case d if d + currentRatio > 1.0 => 1.0 - currentRatio
              case d if d + currentRatio < -1.0 => -1.0 - currentRatio
              case d => d * 0.99
            }
            val tradeAmount = totalBalance * adjustedDiff
            (baseBalance - tradeAmount, instrumentBalance + (tradeAmount / price), turnover + Math.abs(tradeAmount))
          } else balance
        })
    val endPrice = history.priceAt(till.getEpochSecond) //EMA(till.getEpochSecond, timeStep)
    (baseBalance + instrumentBalance*endPrice, 100*endPrice/startPrice, turnover)
  }

//  val results = for (timeFrame <- (50000 to 500000 by 5000).par) yield (timeFrame, run(timeFrame, defaultHysteresis))
//  val (timeFrame, (newBalance, reference, turnover)) = results.reduce((r1,r2) => if (r1._2._1 > r2._2._1) r1 else r2)
//  logger.info(s"timeFrame: $timeFrame, max balance: $newBalance, reference: $reference, turnover: $turnover")

//  val results = for (hysteresis <- (0.0 to 0.7 by 0.01).par) yield (hysteresis, run(defaultTimeFrame, hysteresis))
//  val (hysteresis, (newBalance, reference, turnover)) = results.reduce((r1,r2) => if (r1._2._1 > r2._2._1) r1 else r2)
//  logger.info(s"hysteresis: $hysteresis, max balance: $newBalance, reference: $reference, turnover: $turnover")

    val results = for (multiplier <- (0.1 to 5.0 by 0.01).par) yield (multiplier, run(defaultTimeFrame, defaultHysteresis, multiplier))
    val (multiplier, (newBalance, reference, turnover)) = results.reduce((r1,r2) => if (r1._2._1 > r2._2._1) r1 else r2)
    logger.info(s"multiplier: $multiplier, max balance: $newBalance, reference: $reference, turnover: $turnover")

//  val (newBalance, reference, turnover) = run(defaultTimeFrame, defaultHysteresis, 2.0)
//  logger.info(s"balance: $newBalance, reference: $reference, turnover: $turnover")

}
