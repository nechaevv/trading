package ru.osfb.trading.tradebot

import ru.osfb.trading.{DatabaseComponent, PgDriver}
import ru.osfb.trading.connectors.{BitcoinchartsServiceComponent, BitfinexExchangeComponent, DbHistoryServiceComponentImpl, TradeHistoryService}
import ru.osfb.trading.feeds.BitfinexTradeFeedComponent
import ru.osfb.trading.notification.{NotificationService, PushAllNotificationServiceComponentImpl}
import ru.osfb.webapi.core.{ActorExecutionContextComponentImpl, ActorMaterializerComponentImpl, ActorSystemComponentImpl, ConfigurationComponentImpl}
import ru.osfb.webapi.http.{HttpServer, HttpServerComponentImpl}

/**
  * Created by sgl on 09.04.16.
  */
object ComponentWiring
  extends ConfigurationComponentImpl
    with ActorSystemComponentImpl
    with ActorMaterializerComponentImpl
    with ActorExecutionContextComponentImpl
    with DatabaseComponent
    with BitfinexExchangeComponent
    with BitcoinchartsServiceComponent
    with PushAllNotificationServiceComponentImpl
    with HttpServerComponentImpl
    with BitfinexTradeFeedComponent
    with DbHistoryServiceComponentImpl
{

  override def database: _root_.ru.osfb.trading.PgDriver.api.Database = PgDriver.api.Database.forConfig("database")

  override lazy val bitfinexExchange: BitfinexExchange = new BitfinexExchange
  override lazy val bitcoinchartsService: BitcoinchartsService = new BitcoinchartsService
  override lazy val httpServer: HttpServer = new HttpServerImpl
  override lazy val notificationService: NotificationService = new NotificationServiceImpl
  lazy val bitfinexTradeFeed = new BitfinexTradeFeed("BTCUSD")
  override def tradeHistoryService: TradeHistoryService = new TradeHistoryServiceImpl

}
