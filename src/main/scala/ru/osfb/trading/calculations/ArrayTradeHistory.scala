package ru.osfb.trading.calculations

import scala.annotation.tailrec

/**
  * Created by sgl on 09.04.16.
  */
class ArrayTradeHistory(trades: Iterable[Trade]) extends TradeHistory {
  protected val tradeHistory = trades.toArray

  protected def indexAt(time: Long, top: Boolean): Int = {
    @tailrec
    def indexAtBetween(time: Long, top: Boolean, from: Int, to: Int): Int = if (to - from <= 1) {
      if (top) from else to
    } else {
      val mid = from + ((to-from)>>>1)
      val midTime = tradeHistory(mid).time
      if (midTime == time) mid
      else if (time < midTime) indexAtBetween(time, top, from, mid)
      else indexAtBetween(time, top, mid, to)
    }
    indexAtBetween(time, top, 0, tradeHistory.length-1)
  }

  override def firstTime: Long = tradeHistory.head.time

  override def range(from: Long, till: Long): Iterable[Trade] = {
    tradeHistory.view(indexAt(from, false), indexAt(till, false))
  }

  override def priceAt(time: Long): Double = tradeHistory(indexAt(time, true)) match {
    case Trade(_, amount, quantity) => amount / quantity
  }

  override def lastTime: Long = tradeHistory.last.time
}
