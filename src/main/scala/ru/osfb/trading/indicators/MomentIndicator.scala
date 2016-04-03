package ru.osfb.trading.indicators

import java.time.Instant

import ru.osfb.trading.PgDriver._
import slick.dbio.{Effect, NoStream}

/**
  * Created by sgl on 02.04.16.
  */
trait MomentIndicator {
  def apply(symbol:String, at: Instant): DriverAction[Option[BigDecimal], NoStream, Effect.Read]
}
