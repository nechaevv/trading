package ru.osfb.trading.tradebot

import akka.actor.Props
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.feeds.TradeHistoryDownloaderActor
import ru.osfb.trading.strategies.TrendStrategy

/**
  * Created by sgl on 09.04.16.
  */
object TradeBot extends App with LazyLogging {
  import ComponentWiring._

  logger.info("Booting TradeBot...")

  //httpServer.start(indicatorController)
  val strategy = new TrendStrategy(
    configuration.getLong("tradebot.time-frame"),
    configuration.getLong("tradebot.local-time-factor"),
    configuration.getDouble("tradebot.local-trend-factor"),
    configuration.getDouble("tradebot.open-factor"),
    configuration.getDouble("tradebot.close-factor"),
    configuration.getDouble("tradebot.order-vol-factor"),
    configuration.getLong("tradebot.open-exec-time-factor")
  )

  actorSystem.actorOf(Props(classOf[TradeAdvisorBotActor],
    configuration.getString("tradebot.exchange"),
    configuration.getString("tradebot.symbol"),
    strategy,
    positionsService,
    tradeHistoryService,
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
