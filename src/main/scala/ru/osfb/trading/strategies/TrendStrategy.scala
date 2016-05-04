package ru.osfb.trading.strategies
import play.api.libs.json.{JsObject, Json}
import ru.osfb.trading.calculations.{TradeHistory, TrendFactor, TrendProperties, Volatility}
import ru.osfb.trading.db.TradeType
import ru.osfb.trading.db.TradeType.TradeType
import ru.osfb.trading.strategies.PositionType.PositionType

/**
  * Created by sgl on 18.04.16.
  */
class TrendStrategy
(
  timeFrame: Long,
  avgTimeFactor: Long,
  openFactor: Double,
  closeFactor: Double,
  orderVolFactor: Double,
  orderExecutionTime: Long
) extends TradeStrategy {
  val avgTimeFrame = timeFrame / avgTimeFactor

  override def open(time: Long)(implicit history: TradeHistory): Option[(PositionType, PositionOrder)] = {
    val trendFactor = TrendFactor(time - timeFrame, time, avgTimeFrame).factor
    if (trendFactor > openFactor) Some(PositionType.Long, orderProps(time, TradeType.Buy))
    else if (trendFactor < -openFactor) Some(PositionType.Short, orderProps(time, TradeType.Sell))
    else None
  }

  override def close(time: Long, positionType: PositionType)(implicit history: TradeHistory): Option[PositionOrder] = {
    val trendFactor = TrendFactor(time - timeFrame, time, avgTimeFrame).factor
    positionType match {
      case PositionType.Long if trendFactor < closeFactor => Some(orderProps(time, TradeType.Sell))
      case PositionType.Short if trendFactor > -closeFactor => Some(orderProps(time, TradeType.Buy))
      case _ => None
    }
  }

  def orderProps(time: Long, tradeType: TradeType)(implicit history: TradeHistory) = {
    val pricediff = Volatility(time, orderExecutionTime) * orderVolFactor
    val orderPrice = tradeType match {
      case TradeType.Buy => history.priceAt(time) - pricediff
      case TradeType.Sell => history.priceAt(time) + pricediff
    }
    PositionOrder(orderPrice, orderExecutionTime)
  }

  override def indicators(time: Long)(implicit history: TradeHistory): JsObject = TrendFactor(time - timeFrame, time, avgTimeFrame) match {
    case TrendProperties(startPrice, endPrice, trendFactor) => Json.obj(
      "startPrice" -> startPrice,
      "endPrice" -> endPrice,
      "trendFactor" -> trendFactor
    )
  }

  override val historyDepth: Long = avgTimeFrame * 5 + timeFrame

}
