package ru.osfb.trading.tradebot

import akka.actor.Props
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.feeds.TradeHistoryDownloaderActor

/**
  * Created by sgl on 09.04.16.
  */
object TradeBot extends App with LazyLogging {
  import ComponentWiring._

  logger.info("Booting TradeBot...")

  //httpServer.start(indicatorController)

  actorSystem.actorOf(Props(classOf[TradeAdvisorBotActor],
    configuration.getString("tradebot.exchange"),
    configuration.getString("tradebot.symbol"),
    //indicatorService,
    notificationService,
    configuration))
  actorSystem.actorOf(Props(classOf[TradeHistoryDownloaderActor], bitfinexTradeFeed))

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      logger.info("Shutting down...")
      httpServer.stop()
      actorSystem.terminate()
    }
  })
}
