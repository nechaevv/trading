package ru.osfb.trading.tradebot

import akka.actor.Props
import com.typesafe.scalalogging.LazyLogging

/**
  * Created by sgl on 09.04.16.
  */
object TradeBot extends App with LazyLogging {
  import ComponentWiring._

  logger.info("Booting TradeBot...")

  httpServer.start(indicatorController)

  actorSystem.actorOf(Props(classOf[TradeBotActor],
    configuration.getString("tradebot.symbol"), indicatorService, notificationService, configuration))

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      logger.info("Shutting down...")
      httpServer.stop()
      actorSystem.terminate()
    }
  })
}
