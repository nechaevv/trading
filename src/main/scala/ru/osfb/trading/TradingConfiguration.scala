package ru.osfb.trading

import ru.osfb.webapi.core.ConfigurationComponentImpl

/**
  * Created by sgl on 02.04.16.
  */
object TradingConfiguration
  extends ConfigurationComponentImpl
    with DatabaseComponent
{
  override lazy val database: ru.osfb.trading.PgDriver.api.Database = PgDriver.api.Database.forConfig("database", configuration)
}
