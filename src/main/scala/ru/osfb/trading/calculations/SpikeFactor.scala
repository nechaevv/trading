package ru.osfb.trading.calculations

/**
  * Created by v.a.nechaev on 18.04.2016.
  */
object SpikeFactor {
  def apply(time: Long, avgTimeFrame: Long, spikeTime: Long, changeTimeFactor: Long)(implicit history: TradeHistory) = {
    val price = EMA(time, avgTimeFrame)
    val startPrice = EMA(time - spikeTime, avgTimeFrame)
    val startVol = Volatility(time, spikeTime * changeTimeFactor)
    (price - startPrice)/startVol
  }
}
