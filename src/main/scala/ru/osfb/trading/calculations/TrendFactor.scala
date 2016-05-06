package ru.osfb.trading.calculations

/**
  * Created by sgl on 09.04.16.
  */
object TrendFactor {

  @inline
  def sq(v: Double) = v*v

  def apply(from: Long, till: Long, avgTimeFrame: Long)(implicit history: TradeHistory): TrendProperties = {
    //val startPrice = EMA(from, avgTimeFrame)
    val startPrice = EMA(from, avgTimeFrame)
    val endPrice = EMA(till, avgTimeFrame)
    val delta = endPrice - startPrice
    val delta2 = sq(delta)
    val k = delta/(till - from)
    history.range(from, till).foldLeft((0.0, 0.0))((acc, trd) => {
      if (trd.quantity != 0) (acc._1 + trd.quantity * sq(trd.amount/trd.quantity - (startPrice + k * (trd.time - from))),
        acc._2 + trd.quantity * delta2) else acc
    }) match {
      case (amt, qty) =>
        val factor = Math.sqrt(qty / amt) * Math.signum(endPrice - startPrice)
        TrendProperties(startPrice, endPrice, factor)
    }
  }

}

case class TrendProperties(startPrice: Double, endPrice: Double, factor: Double)
