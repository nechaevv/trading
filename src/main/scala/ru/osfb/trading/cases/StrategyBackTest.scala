package ru.osfb.trading.cases

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.TradeHistory
import ru.osfb.trading.db.TradeType
import ru.osfb.trading.db.TradeType.TradeType
import ru.osfb.trading.strategies.PositionType.PositionType
import ru.osfb.trading.strategies.{PositionOrder, PositionType, TradeStrategy}

import scala.collection.parallel.ParSeq

/**
  * Created by sgl on 02.05.16.
  */
class StrategyBackTest extends LazyLogging {
  def run(strategy: TradeStrategy, from: Long, till: Long, timeStep: Long)(implicit history: TradeHistory):BacktestStatistics = {
    case class RunState(openPosition: Option[BacktestPosition], statistics: BacktestStatistics)
    def foldFn(acc: RunState, indicators: strategy.StrategyIndicators) = acc match {
      case RunState(pos, stat) => pos match {
        case None => strategy.open(indicators) match {
          case None => acc
          case Some((positionType, po)) =>
            //val price = history.priceAt(time)
            val price = executeOrder(indicators.time, positionType match {
              case PositionType.Long => TradeType.Buy
              case PositionType.Short => TradeType.Sell
            }, po)
            //logger.info(s"Opening $positionType position at $price on ${Instant.ofEpochSecond(time)}")
            RunState(Some(BacktestPosition(indicators.time, price, positionType)), stat)
        }
        case Some(position) => strategy.close(indicators, position.positionType) match {
          case Some(po) =>
            //val price = history.priceAt(time)
            val price = executeOrder(indicators.time, position.positionType match {
              case PositionType.Long => TradeType.Sell
              case PositionType.Short => TradeType.Buy
            }, po)
            //logger.info(s"Closing ${position.positionType} position at $price on ${Instant.ofEpochSecond(time)}")
            val profit = (position.positionType match {
              case PositionType.Long => price - position.openPrice
              case PositionType.Short => position.openPrice - price
            }) / position.openPrice
            val newStat = if (profit > 0) BacktestStatistics(PositionStatistics(
              stat.succeeded.count + 1, stat.succeeded.profit + profit, stat.succeeded.time + indicators.time - position.openTime
            ), stat.failed) else BacktestStatistics(stat.succeeded, PositionStatistics(
              stat.failed.count + 1, stat.failed.profit + profit, stat.failed.time + indicators.time - position.openTime
            ))
            RunState(None, newStat)
          case None => acc
        }
      }
    }
    val zeroStatistics = PositionStatistics(0,0,0)
    (from to till by timeStep)
      .par.map(strategy.indicators(_))
      //.seq.sortBy(_.time)
      .foldLeft(RunState(None, BacktestStatistics(zeroStatistics,zeroStatistics)))(foldFn).statistics
  }
  def optimize[T](strategyFactory: T => TradeStrategy, params: Seq[T],
               from: Long, till: Long, timeStep: Long)(implicit history: TradeHistory):ParSeq[(T, BacktestStatistics)] = {
    params.par.map(param => param -> run(strategyFactory(param),from, till, timeStep))
  }

  def executeOrder(time: Long, orderType: TradeType, po: PositionOrder)(implicit history: TradeHistory):Double = {
    val execStat = history.periodStatistics(time, time + po.executionTime)
    val price = orderType match {
      case TradeType.Buy => if (execStat.min <= po.price) po.price else execStat.close
      case TradeType.Sell => if (execStat.max >= po.price) po.price else execStat.close
    }
    //logger.info(s"${Instant.ofEpochSecond(time)} Order $orderType executed at $price ${if(price == po.price) "HIT" else "MISS"}, diff: ${price - execStat.open}")
    price
  }

}

case class BacktestPosition(openTime: Long, openPrice: Double, positionType: PositionType)
case class PositionStatistics(count: Int, profit: Double, time: Long)
case class BacktestStatistics(succeeded: PositionStatistics, failed: PositionStatistics)
