package ru.osfb.trading.tradebot

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.pattern.pipe
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.connectors.TradeHistoryService
import ru.osfb.trading.feeds.{HistoryUpdateEvent, HistoryUpdateEventBus}
import ru.osfb.trading.notification.NotificationService
import ru.osfb.trading.strategies.{TradeStrategy, TrendStrategy}

import scala.concurrent.duration
import scala.concurrent.duration.Duration

/**
  * Created by sgl on 10.04.16.
  */
class TradeBotActor
(
  exchange: String,
  symbol: String,
  strategy: TradeStrategy,
  tradeHistoryService: TradeHistoryService,
  notificationService: NotificationService,
  configuration: Config
) extends Actor with LazyLogging {


  implicit def executionContext = context.dispatcher

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    HistoryUpdateEventBus.subscribe(self, HistoryUpdateEvent(exchange, symbol))
    logger.info(s"TradeBot actor started for $symbol at $exchange")
    if (configuration.hasPath("tradebot.notify-start") && configuration.getBoolean("tradebot.notify-start")) {
      notificationService.notify(s"Started for $symbol at $exchange")
    }
  }

  var stateOpt: Option[Indicators] = None

  override def receive: Receive = {
    case _:HistoryUpdateEvent =>
      logger.trace("Analyzing new history records")
      val now = System.currentTimeMillis() / 1000
      tradeHistoryService.loadHistory(exchange, symbol, )
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
