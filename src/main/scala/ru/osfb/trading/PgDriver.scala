package ru.osfb.trading

import java.time.{Duration, Instant}

import com.github.tminglei.slickpg.{ExPostgresDriver, PgDate2Support}

/**
  * Created by sgl on 20.03.16.
  */
object PgDriver extends ExPostgresDriver
  with PgDate2Support {
  class PgApi extends API with DateTimeImplicits {
    val exp = SimpleFunction.unary[BigDecimal, BigDecimal]("EXP")
    val dateDiffSeconds = SimpleFunction.binary[Instant, Instant, BigDecimal]("DATEDIFF_SECONDS")
    val intervalSeconds = SimpleFunction.unary[Duration, BigDecimal]("INTERVAL_SECONDS")
  }
  override val api = new PgApi
}

