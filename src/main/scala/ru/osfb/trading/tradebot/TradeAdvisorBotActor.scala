package ru.osfb.trading.tradebot

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.pattern.pipe
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.{ArrayTradeHistory, Trade}
import ru.osfb.trading.connectors.TradeHistoryService
import ru.osfb.trading.feeds.{HistoryUpdateEvent, HistoryUpdateEventBus}
import ru.osfb.trading.notification.NotificationService
import ru.osfb.trading.strategies.{Position, TradeStrategy, TrendStrategy}

import scala.concurrent.duration
import scala.concurrent.duration.Duration

/**
  * Created by sgl on 10.04.16.
  */
class TradeAdvisorBotActor
(
  exchange: String,
  symbol: String,
  strategy: TradeStrategy,
  tradeHistoryService: TradeHistoryService,
  notificationService: NotificationService,
  configuration: Config
) extends Actor with LazyLogging {

  val name = self.path.name

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
  var position: Option[Position] = None

  override def receive: Receive = {
    case _:HistoryUpdateEvent =>
      logger.trace("Analyzing new history records")
      val now = System.currentTimeMillis() / 1000
      tradeHistoryService.loadLatest(exchange, symbol, strategy.historyDepth) pipeTo self
    case historyRecords: Seq[Trade] if historyRecords.nonEmpty =>
      implicit val history = new ArrayTradeHistory(historyRecords)
      val lastTime = history.lastTime
      val lastPrice = history.priceAt(lastTime)
      position match {
        case Some(pos) => if (strategy.close(lastTime, pos.positionType)) {
          val openTime = lastTime - pos.time
          notificationService.notify(s"${pos.positionType.toString} - Close (open at ${pos.price}, close at $lastPrice, opened for )")
          position = None
        }
        case None => strategy.open(lastTime) foreach { positionType =>
          notificationService.notify(s"${positionType.toString} - Open ($lastPrice)")
          position = Some(Position(lastTime, lastPrice, 1, positionType))
        }
      }
  }
}

case object DoAnalyze
