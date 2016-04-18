package ru.osfb.trading.calculations

/**
  * Created by v.a.nechaev on 18.04.2016.
  */
object SpikeFactor {
  def apply(history: TradeHistory, time: Long, avgTimeFrame: Long, spikeTime: Long, changeTimeFactor: Long) = {
    val price = EMA(history, time, avgTimeFrame)
    val startPrice = EMA(history, time - spikeTime, avgTimeFrame)
    val startVol = Volatility(history, time, spikeTime * changeTimeFactor)
    (price - startPrice)/startVol
  }
}
