package ru.osfb.trading.calculations

/**
  * Created by sgl on 09.04.16.
  */
object Volatility {
  @inline
  def sq(v: Double) = v*v
  def apply(time: Long, timeFrame: Long)(implicit history: TradeHistory) = {
    val mean = EMA(time, timeFrame)
    history.range(time - timeFrame*5, time)
      .foldLeft((0.0, 0.0))((acc, trd) => {
        val coeff = trd.quantity / Math.exp((time - trd.time).toDouble/timeFrame)
        (acc._1 + (sq(trd.price - mean) *  coeff),
          acc._2 + coeff)
      }) match {
      case (amt, qty) => Math.sqrt(amt / qty)
    }
  }
}
