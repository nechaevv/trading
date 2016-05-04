package ru.osfb.trading.tradebot

import ru.osfb.trading.connectors.{BitcoinchartsServiceComponent, BitfinexExchangeComponent}
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
    with BitfinexExchangeComponent
    with BitcoinchartsServiceComponent
    with IndicatorControllerComponent
    with IndicatorServiceComponent
    with PushAllNotificationServiceComponentImpl
    with HttpServerComponentImpl
    with BitfinexTradeFeedComponent
{
  override lazy val bitfinexExchange: BitfinexExchange = new BitfinexExchange
  override lazy val bitcoinchartsService: BitcoinchartsService = new BitcoinchartsService
  override lazy val httpServer: HttpServer = new HttpServerImpl
  override lazy val indicatorService: IndicatorServiceImpl = new IndicatorServiceImpl
  override lazy val notificationService: NotificationService = new NotificationServiceImpl
  lazy val bitfinexTradeFeed = new BitfinexTradeFeed("BTCUSD")
}
