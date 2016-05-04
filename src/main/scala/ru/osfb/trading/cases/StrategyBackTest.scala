package ru.osfb.trading.cases

import java.time.Instant

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.{TradeHistory, Volatility}
import ru.osfb.trading.strategies.PositionType.PositionType
import ru.osfb.trading.strategies.{PositionType, TradeStrategy}

import scala.collection.immutable.NumericRange
import scala.collection.parallel.ParSeq

/**
  * Created by sgl on 02.05.16.
  */
class StrategyBackTest extends LazyLogging {
  def run(strategy: TradeStrategy, from: Long, till: Long, timeStep: Long, orderExecutionTime: Long)(implicit history: TradeHistory):BacktestStatistics = {
    case class RunState(openPosition: Option[BacktestPosition], statistics: BacktestStatistics)
    def foldFn(acc: RunState, time: Long) = acc match {
      case RunState(pos, stat) => pos match {
        case None => strategy.open(time) match {
          case None => acc
          case Some(positionType) =>
            //val price = history.priceAt(time)
            val lastVol = Volatility(time, orderExecutionTime)
            val execStat = periodStatistics(time, time + orderExecutionTime)
            val price = positionType match {
              case PositionType.Long => if (execStat.min < execStat.open - lastVol) execStat.open - lastVol else execStat.close
              case PositionType.Short => if (execStat.max > execStat.open + lastVol) execStat.open + lastVol else execStat.close
            }
            //logger.info(s"Opening $positionType position at $price on ${Instant.ofEpochSecond(time)}")
            RunState(Some(BacktestPosition(time, price, positionType)), stat)
        }
        case Some(position) => if (strategy.close(time, position.positionType)) {
          //val price = history.priceAt(time)
          val lastVol = Volatility(time, orderExecutionTime)
          val execStat = periodStatistics(time, time + orderExecutionTime)
          val price = position.positionType match {
            case PositionType.Long => if (execStat.max > execStat.open + lastVol) execStat.open + lastVol else execStat.close
            case PositionType.Short => if (execStat.min < execStat.open - lastVol) execStat.open - lastVol else execStat.close
          }
          //logger.info(s"Closing ${position.positionType} position at $price on ${Instant.ofEpochSecond(time)}")
          val profit = (position.positionType match {
            case PositionType.Long => price - position.openPrice
            case PositionType.Short => position.openPrice - price
          }) / position.openPrice
          val newStat = if (profit > 0) BacktestStatistics(PositionStatistics(
            stat.succeeded.count + 1, stat.succeeded.profit + profit, stat.succeeded.time + time - position.openTime
          ), stat.failed) else BacktestStatistics(stat.succeeded, PositionStatistics(
            stat.failed.count + 1, stat.failed.profit + profit, stat.failed.time + time - position.openTime
          ))
          RunState(None, newStat)
        } else acc
      }
    }
    val zeroStatistics = PositionStatistics(0,0,0)
    (from to till by timeStep).foldLeft(RunState(None, BacktestStatistics(zeroStatistics,zeroStatistics)))(foldFn).statistics
  }
  def optimize[T](strategyFactory: T => TradeStrategy, params: Seq[T],
               from: Long, till: Long, timeStep: Long, orderExecutionTime: Long)(implicit history: TradeHistory):ParSeq[(T, Double)] = {
    params.par.map(param => run(strategyFactory(param),from, till, timeStep, orderExecutionTime) match {
      case BacktestStatistics(succeeded, failed) => param -> (succeeded.profit + failed.profit)
    })
  }
  def periodStatistics(from: Long, till: Long)(implicit history: TradeHistory): PeriodStatistics = {
    val priceHistory = history.range(from, till)
    val open = priceHistory.head.price
    val close = priceHistory.last.price
    priceHistory.foldLeft(PeriodStatistics(open, close, if (open>close) close else open, if (open>close) open else close, 0.0))((acc, trade) => {
      val price = trade.price
      acc.copy(min = if (price < acc.min) price else acc.min, max = if (price > acc.max) price else acc.max, volume = acc.volume + trade.quantity)
    })
  }
}

case class BacktestPosition(openTime: Long, openPrice: Double, positionType: PositionType)
case class PositionStatistics(count: Int, profit: Double, time: Long)
case class BacktestStatistics(succeeded: PositionStatistics, failed: PositionStatistics)
case class PeriodStatistics(open: Double, close: Double, min: Double, max: Double, volume: Double)