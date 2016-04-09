package ru.osfb.trading.calculations

/**
  * Created by sgl on 09.04.16.
  */
object Volatility {
  def sq(v: Double) = v*v
  def apply(history: TradeHistory, time: Long, timeFrame: Long) = {
    val mean = EMA(history, time, timeFrame)
    history.range(time - timeFrame*5, time)
      .foldLeft((0.0, 0.0))((acc, trd) => {
        val coeff = Math.exp((time - trd.time).toDouble/timeFrame)
        (acc._1 + trd.quantity * sq(trd.amount/trd.quantity - mean) / coeff,
          acc._2 + trd.quantity / coeff)
      }) match {
      case (amt, qty) => Math.sqrt(amt / qty)
    }
  }
}
