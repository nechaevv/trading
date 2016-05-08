package ru.osfb.trading.calculations

/**
  * Created by v.a.nechaev on 06.05.2016.
  */
object CovarianceTrendFactor {
  def apply(time: Long, timeFrame: Long)(implicit history: TradeHistory): Double = {
    //Exponential
    val points = history.range(time - timeFrame*5, time).map(t => {
      val dt = time - t.time
      (t.time, t.price, t.quantity/Math.exp(dt/timeFrame))
    })
    //Constant
//    val points = history.range(time - timeFrame, time).map(t => {
//      val dt = time - t.time
//      (t.time, t.price, t.quantity)
//    })
    val (tT, tP, tC) = points.foldLeft((0.0,0.0,0.0))((acc, v) => {
      val (accT,accP,accC) = acc
      val (t,p,c) = v
      (accT + c*t, accP + c*p, accC + c)
    })
    val mT = tT/tC
    val mP = tP/tC
    val (dT,dP,cov) = points.foldLeft((0.0,0.0,0.0))((acc,v) => {
      val (accT,accP,accC) = acc
      val (t,p,c) = v
      (accT + c*sq(t-mT), accP + c*sq(p-mP), accC + c*(t-mT)*(p-mP))
    })
    cov/Math.sqrt(dT*dP)
  }
  @inline
  def sq(v: Double) = v*v

}
