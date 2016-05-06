package ru.osfb.trading.calculations

/**
  * Created by v.a.nechaev on 06.05.2016.
  */
object CovarianceTrendFactor {
  def apply(time: Long, timeFrame: Long)(implicit history: TradeHistory): Double = {
    val points = history.range(time - timeFrame*5, time).map(t => {
      val dt = time - t.time
      (t.time, t.price, t.quantity/Math.exp(dt/timeFrame))
    })
    ???
  }
}
