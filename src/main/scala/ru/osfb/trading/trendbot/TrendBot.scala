package ru.osfb.trading.trendbot

import akka.actor.Props
import com.typesafe.scalalogging.LazyLogging

/**
  * Created by sgl on 09.04.16.
  */
object TrendBot extends App with LazyLogging {
  import ComponentWiring._

  logger.info("Booting TrendBot...")

  httpServer.start(indicatorController)
  actorSystem.actorOf(Props(classOf[TrendbotActor],
    configuration.getString("trendbot.symbol"), indicatorService, notificationService, configuration))

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      logger.info("Shutting down...")
      httpServer.stop()
      actorSystem.terminate()
    }
  })
}
