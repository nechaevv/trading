package ru.osfb.trading.strategies

import ru.osfb.trading.calculations.TradeHistory
import ru.osfb.trading.strategies.PositionType.PositionType

/**
  * Created by sgl on 18.04.16.
  */
trait TradeStrategy {
  type StrategyIndicators <: TradeIndicators
  def open(indicators: StrategyIndicators): Option[(PositionType, PositionOrder)]
  def close(indicators: StrategyIndicators, positionType: PositionType): Option[PositionOrder]
  def indicators(time: Long)(implicit history: TradeHistory): StrategyIndicators
  def historyDepth: Long
}

object PositionType extends Enumeration {
  type PositionType = Value
  val Long = Value("L")
  val Short = Value("S")
}

case class PositionOrder(price: Double, executionTime: Long)

trait TradeIndicators {
  def time: Long
}