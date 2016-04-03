package ru.osfb.trading.indicators

import java.time.Instant

import ru.osfb.trading.PgDriver.DriverAction
import slick.dbio.{Effect, NoStream}

/**
  * Created by sgl on 02.04.16.
  */
trait IntervalIndicator {
  def apply(symbol:String, from: Instant, till: Instant): DriverAction[Option[BigDecimal], NoStream, Effect.Read]
}
