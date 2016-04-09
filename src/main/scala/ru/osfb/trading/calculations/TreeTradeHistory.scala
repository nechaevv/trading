package ru.osfb.trading.calculations

import scala.collection.immutable.TreeMap

/**
  * Created by sgl on 08.04.16.
  */
class TreeTradeHistory(trades: Iterable[Trade]) extends TradeHistory {
  protected val tradeTree = TreeMap(trades
    .groupBy(_.time)
    .map({
      case (time, timeTrades) => time -> Trade(time,
        timeTrades.map(_.amount).sum, timeTrades.map(_.quantity).sum)
    }).toSeq :_*)

  override def firstTime: Long = tradeTree.firstKey
  override def lastTime: Long = tradeTree.lastKey
  override def range(from: Long, till: Long): Iterable[Trade] = tradeTree.range(from, till).values
  override def priceAt(time: Long): Double = tradeTree.to(time).last._2 match {
    case Trade(_, amt, qty) => amt/qty
  }
}
