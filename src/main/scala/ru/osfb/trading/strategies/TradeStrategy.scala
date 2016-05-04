package ru.osfb.trading.strategies

import play.api.libs.json.JsObject
import ru.osfb.trading.calculations.TradeHistory
import ru.osfb.trading.db.TradeType.TradeType
import ru.osfb.trading.strategies.PositionType.PositionType

/**
  * Created by sgl on 18.04.16.
  */
trait TradeStrategy {
  def open(time: Long)(implicit history: TradeHistory): Option[(PositionType, PositionOrder)]
  def close(time: Long, positionType: PositionType)(implicit history: TradeHistory): Option[PositionOrder]
  def indicators(time: Long)(implicit history: TradeHistory): JsObject
  def historyDepth: Long
}

object PositionType extends Enumeration {
  type PositionType = Value
  val Long = Value("L")
  val Short = Value("S")
}

case class PositionOrder(price: Double, executionTime: Long)
