package ru.osfb.trading

/**
  * Created by sgl on 20.03.16.
  */
trait DatabaseComponent {
  def database: PgDriver.api.Database
}
