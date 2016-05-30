package ru.osfb.trading.cases

import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.{ArrayTradeHistory, CovarianceTrendFactor, EMA, TrendFactor}
import ru.osfb.trading.connectors.BfxData

/**
  * Created by v.a.nechaev on 30.05.2016.
  */
object BalancingStrategyTest extends App with LazyLogging {
  val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
  def toInstant(s: String) = Instant.from(LocalDateTime.from(dateFormatter.parse(s)).atZone(ZoneId.systemDefault()))

  val from = toInstant(args(1))
  val till = toInstant(args(2))
  val timeStep = args(3).toLong
  val timeFrame = args(4).toLong

  val fetchTimeStart = from minusSeconds 5*timeFrame

  val trades =
  //Await.result(CsvHistoryStore.loadHistory(args(0), fetchTimeStart, till), 1.hour)
    BfxData.loadVwapData("BTCUSD", fetchTimeStart.toEpochMilli / 1000, till.toEpochMilli / 1000)
  //  Finam.loadCsvTrades(args(0))
  implicit val history = new ArrayTradeHistory(trades)

  logger.info(s"Running backtest from $from till $till")

  val (baseBalance, instrumentBalance) = (from.getEpochSecond to till.getEpochSecond by timeStep).foldLeft((100.0, 0.0))((balance, time) => {
    val (baseBalance, instrumentBalance) = balance
    val tf = CovarianceTrendFactor(time, timeFrame)
    val price = EMA(time, timeStep)
    val diff = ((tf + 1.0)/2) - (baseBalance/(baseBalance + instrumentBalance))
    val tradeAmount = (baseBalance + instrumentBalance*price)*diff
    (baseBalance - tradeAmount, instrumentBalance + (tradeAmount/price))
  })
  val price = EMA(till.getEpochSecond, timeStep)

  logger.info(s"Resulting balance: ${baseBalance + instrumentBalance*price} ")

}
