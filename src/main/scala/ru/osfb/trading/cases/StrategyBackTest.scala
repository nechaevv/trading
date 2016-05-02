package ru.osfb.trading.cases

import java.time.Instant

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.TradeHistory
import ru.osfb.trading.strategies.PositionType.PositionType
import ru.osfb.trading.strategies.{PositionType, TradeStrategy}

/**
  * Created by sgl on 02.05.16.
  */
class StrategyBackTest(strategy: TradeStrategy) extends LazyLogging {
  def run(from: Long, till: Long, timeStep: Long)(implicit history: TradeHistory):BacktestStatistics = {
    case class RunState(openPosition: Option[BacktestPosition], statistics: BacktestStatistics)
    def foldFn(acc: RunState, time: Long) = acc match {
      case RunState(pos, stat) => pos match {
        case None => strategy.open(time) match {
          case None => RunState(None,stat)
          case Some(positionType) =>
            val price = history.priceAt(time)
            logger.info(s"Opening $positionType position at $price on ${Instant.ofEpochSecond(time)}")
            RunState(Some(BacktestPosition(price, positionType)), stat)
        }
        case Some(position) => if (strategy.close(time, position.positionType)) {
          val price = history.priceAt(time)
          logger.info(s"Closing ${position.positionType} position at $price on ${Instant.ofEpochSecond(time)}")
          val profit = (position.positionType match {
            case PositionType.Long => price - position.openPrice
            case PositionType.Short => position.openPrice - price
          }) / position.openPrice
          val newStat = if (profit > 0) BacktestStatistics(PositionStatistics(
            stat.succeded.count + 1, stat.succeded.profit + profit
          ), stat.failed) else BacktestStatistics(stat.succeded, PositionStatistics(
            stat.failed.count + 1, stat.failed.profit + profit
          ))
          RunState(None, newStat)
        } else RunState(Some(position), stat)
      }


    }
    val zeroStatistics = PositionStatistics(0,0)
    (from to till by timeStep).foldLeft(RunState(None, BacktestStatistics(zeroStatistics,zeroStatistics)))(foldFn).statistics
  }
}

case class BacktestPosition(openPrice: Double, positionType: PositionType)
case class PositionStatistics(count: Int, profit: Double)
case class BacktestStatistics(succeded: PositionStatistics, failed: PositionStatistics)