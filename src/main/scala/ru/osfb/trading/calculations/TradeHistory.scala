package ru.osfb.trading.calculations

/**
  * Created by sgl on 08.04.16.
  */
trait TradeHistory {
  def firstTime: Long
  def lastTime: Long
  def range(from: Long, till: Long): Iterable[Trade]
}
