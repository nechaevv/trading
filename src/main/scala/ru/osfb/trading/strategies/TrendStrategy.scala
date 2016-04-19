package ru.osfb.trading.strategies
import play.api.libs.json.{JsObject, Json}
import ru.osfb.trading.calculations.{TradeHistory, TrendFactor, TrendProperties}
import ru.osfb.trading.strategies.PositionType.PositionType

/**
  * Created by sgl on 18.04.16.
  */
class TrendStrategy
(
  timeFrame: Long,
  avgTimeFactor: Long,
  openFactor: Double,
  closeFactor: Double
) extends TradeStrategy {
  val avgTimeFrame = timeFrame / avgTimeFactor

  override def open(time: Long)(implicit history: TradeHistory): Option[PositionType] = {
    val trendFactor = TrendFactor(time - timeFrame, time, avgTimeFrame).factor
    if (trendFactor > openFactor) Some(PositionType.Long)
    else if (trendFactor < -openFactor) Some(PositionType.Short)
    else None
  }

  override def close(time: Long, positionType: PositionType)(implicit history: TradeHistory): Boolean = {
    val trendFactor = TrendFactor(time - timeFrame, time, avgTimeFrame).factor
    positionType match {
      case PositionType.Long => trendFactor < closeFactor
      case PositionType.Short => trendFactor > -closeFactor
    }
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
