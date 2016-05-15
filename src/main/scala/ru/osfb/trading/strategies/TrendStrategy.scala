package ru.osfb.trading.strategies
import ru.osfb.trading.calculations._
import ru.osfb.trading.db.TradeType
import ru.osfb.trading.db.TradeType.TradeType
import ru.osfb.trading.strategies.PositionType.PositionType

/**
  * Created by sgl on 18.04.16.
  */
class TrendStrategy
(
  val timeFrame: Long,
  val localTimeFactor: Long,
  val localTrendFactor: Double,
  val openFactor: Double,
  val closeFactor: Double,
  val orderVolFactor: Double,
  val orderExecutionTimeFactor: Long
) extends TradeStrategy {

  override type StrategyIndicators = TrendIndicators

  override def open(indicators: TrendIndicators): Option[(PositionType, PositionOrder)] = {
    val trendFactor = (1.0 - Math.abs(localTrendFactor))*indicators.trend + localTrendFactor*indicators.localTrend
    if (trendFactor > openFactor) Some(PositionType.Long, orderProps(indicators, TradeType.Buy))
    else if (trendFactor < -openFactor) Some(PositionType.Short, orderProps(indicators, TradeType.Sell))
    else None
  }

  override def close(indicators: TrendIndicators, positionType: PositionType): Option[PositionOrder] = {
    val trendFactor = (1.0 - localTrendFactor)*indicators.trend + localTrendFactor*indicators.localTrend
    positionType match {
      case PositionType.Long if trendFactor < closeFactor => Some(orderProps(indicators, TradeType.Sell))
      case PositionType.Short if trendFactor > -closeFactor => Some(orderProps(indicators, TradeType.Buy))
      case _ => None
    }
  }

  override def indicators(time: Long)(implicit history: TradeHistory): TrendIndicators = TrendIndicators(
    time,
    CovarianceTrendFactor(time, timeFrame),
    CovarianceTrendFactor(time, localTimeFrame),
    Volatility(time, orderExecutionTime),
    history.priceAt(time)
  )

  override val historyDepth: Long = timeFrame * 5

  protected def orderProps(indicators: TrendIndicators, tradeType: TradeType) = {
    val pricediff = indicators.execVolatility * orderVolFactor
    val orderPrice = tradeType match {
      case TradeType.Buy => indicators.price - pricediff
      case TradeType.Sell => indicators.price + pricediff
    }
    PositionOrder(orderPrice, orderExecutionTime)
  }
  protected val localTimeFrame = timeFrame / localTimeFactor
  protected val orderExecutionTime = timeFrame / orderExecutionTimeFactor

}

case class TrendIndicators(time: Long, trend: Double, localTrend: Double, execVolatility: Double, price: Double) extends TradeIndicators