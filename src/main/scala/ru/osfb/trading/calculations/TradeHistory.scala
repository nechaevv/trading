package ru.osfb.trading.calculations

/**
  * Created by sgl on 08.04.16.
  */
trait TradeHistory {
  def firstTime: Long
  def lastTime: Long
  def range(from: Long, till: Long): Iterable[Trade]
  def priceAt(time: Long): Double

  def periodStatistics(from: Long, till: Long): PeriodStatistics = {
    val priceHistory = range(from, till)
    val open = if(priceHistory.isEmpty) priceAt(from) else priceHistory.head.price
    val close = if(priceHistory.isEmpty) priceAt(till) else priceHistory.last.price
    priceHistory.foldLeft(PeriodStatistics(open, close, if (open>close) close else open, if (open>close) open else close, 0.0))((acc, trade) => {
      val price = trade.price
      acc.copy(min = if (price < acc.min) price else acc.min, max = if (price > acc.max) price else acc.max, volume = acc.volume + trade.quantity)
    })
  }
}

case class PeriodStatistics(open: Double, close: Double, min: Double, max: Double, volume: Double)