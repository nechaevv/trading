package ru.osfb.trading.tradebot

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.pattern.pipe
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.notification.NotificationService
import ru.osfb.trading.strategies.{TradeStrategy, TrendStrategy}

import scala.concurrent.duration
import scala.concurrent.duration.Duration

/**
  * Created by sgl on 10.04.16.
  */
class TradeBotActor
(
  symbol: String,
  strategy: TradeStrategy,
  notificationService: NotificationService,
  configuration: Config
) extends Actor with LazyLogging {


  lazy val openAt = configuration.getDouble("trendbot.open-at")
  lazy val closeAt = configuration.getDouble("trendbot.close-at")
  lazy val timeFrame = configuration.getDuration("trend-factor.avg-time-frame", TimeUnit.SECONDS)

  implicit def executionContext = context.dispatcher

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    val scheduleInterval = timeFrame/12
    context.system.scheduler.schedule(Duration.Zero, Duration(scheduleInterval, duration.SECONDS), self, DoAnalyze)
    logger.info(s"TrendBot actor started for $symbol with poll interval $scheduleInterval seconds")
    if (configuration.hasPath("tradebot.notify-start") && configuration.getBoolean("tradebot.notify-start"))
    notificationService.notify(s"Started for $symbol")
  }

  var stateOpt: Option[Indicators] = None

  override def receive: Receive = {
    case DoAnalyze =>
      logger.trace("Polling")
      indicatorService.computeIndicators(symbol) pipeTo self
    case newState@Indicators(price, trendFactor) =>
      for (state <- stateOpt) if (state.trendFactor <= openAt && newState.trendFactor > openAt) {
        notificationService.notify(symbol + "Long - Open")
      } else if (state.trendFactor >= -openAt && newState.trendFactor < -openAt) {
        notificationService.notify(symbol + "Short - Open")
      } else if (state.trendFactor >= closeAt && newState.trendFactor < closeAt) {
        notificationService.notify(symbol + "Long - Close")
      } else if (state.trendFactor <= -closeAt && newState.trendFactor > -closeAt) {
        notificationService.notify(symbol + "Short - Close")
      }
      stateOpt = Some(newState)
  }
}

case object DoAnalyze
