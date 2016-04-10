package ru.osfb.trading.calculations

/**
  * Created by sgl on 09.04.16.
  */
object TrendFactor {
  def sq(v: Double) = v*v
  def apply(history: TradeHistory,
            from: Long, till: Long,
            avgTimeFrame: Long): TrendProperties = {
    val startPrice = EMA(history, from, avgTimeFrame)
    val endPrice = EMA(history, till, avgTimeFrame)
    val delta = endPrice - startPrice
    val delta2 = sq(delta)
    val k = delta/(till - from)
    history.range(from, till).foldLeft((0.0, 0.0))((acc, trd) => {
      (acc._1 + trd.quantity * sq(trd.amount/trd.quantity - (startPrice + k * (trd.time - from))),
        acc._2 + trd.quantity * delta2)
    }) match {
      case (amt, qty) =>
        val factor = Math.sqrt(qty / amt) * Math.signum(endPrice - startPrice)
        TrendProperties(startPrice, endPrice, factor)
    }
  }
}

case class TrendProperties(startPrice: Double, endPrice: Double, factor: Double)
