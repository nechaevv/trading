package ru.osfb.trading.calculations

/**
  * Created by sgl on 09.04.16.
  */
object Average {
  def apply(from: Long, till: Long)(implicit history: TradeHistory): Double = history
      .range(from, till).foldLeft((0.0,0.0))((acc, trd) => (acc._1 + trd.amount, acc._2 + trd.quantity)) match {
    case (amt, qty) => amt / qty
  }
}
