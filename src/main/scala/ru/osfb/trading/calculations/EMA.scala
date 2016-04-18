package ru.osfb.trading.calculations

/**
  * Created by sgl on 09.04.16.
  */
object EMA {
  def apply(time: Long, timeFrame: Long)(implicit history: TradeHistory): Double = history
    .range(time - timeFrame*5, time)
    .foldLeft((0.0, 0.0))((acc, trd) => {
      val coeff = Math.exp((time - trd.time).toDouble/timeFrame)
      (acc._1 + trd.amount/coeff, acc._2 + trd.quantity/coeff)
    }) match {
    case (amt, qty) => amt / qty
  }
}
