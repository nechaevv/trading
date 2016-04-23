package ru.osfb.trading.tradebot

import java.time.Instant

import akka.actor.Actor
import akka.pattern.pipe
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.{ArrayTradeHistory, Trade}
import ru.osfb.trading.connectors.{PositionsService, TradeHistoryService}
import ru.osfb.trading.db.Position
import ru.osfb.trading.feeds.{HistoryUpdateEvent, HistoryUpdateEventBus}
import ru.osfb.trading.notification.NotificationService
import ru.osfb.trading.strategies.TradeStrategy
import ru.osfb.trading.tradebot.TradeAdvisorBotActor.{DoAnalyze, InitPosition}

/**
  * Created by sgl on 10.04.16.
  */
class TradeAdvisorBotActor
(
  exchange: String,
  symbol: String,
  strategy: TradeStrategy,
  positionsService: PositionsService,
  tradeHistoryService: TradeHistoryService,
  notificationService: NotificationService,
  configuration: Config
) extends Actor with LazyLogging {

  val name = self.path.name

  implicit def executionContext = context.dispatcher

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    logger.info(s"TradeBot actor started for $symbol at $exchange")
    if (configuration.hasPath("tradebot.notify-start") && configuration.getBoolean("tradebot.notify-start")) {
      notificationService.notify(s"Started for $symbol at $exchange")
    }
    val positionFuture = positionsService.findOpenPositions(name).map(_.headOption)
    positionFuture pipeTo self
    positionFuture onComplete { _ =>
      HistoryUpdateEventBus.subscribe(self, HistoryUpdateEvent(exchange, symbol))
    }
  }

  var stateOpt: Option[Indicators] = None
  var position: Option[Position] = None

  override def receive: Receive = {
    case InitPosition(pos) =>
      position = pos
    case _:HistoryUpdateEvent =>
      logger.trace("Analyzing new history records")
      val now = System.currentTimeMillis() / 1000
      tradeHistoryService.loadLatest(exchange, symbol, strategy.historyDepth)
        .map(DoAnalyze) pipeTo self
    case DoAnalyze(historyRecords) if historyRecords.nonEmpty =>
      implicit val history = new ArrayTradeHistory(historyRecords)
      val lastTime = history.lastTime
      val lastPrice = history.priceAt(lastTime)
      //TODO: stash history update events while position is closing/opening
      position match {
        case Some(pos) => if (strategy.close(lastTime, pos.positionType)) {
          val openTime = lastTime - pos.openedAt.getEpochSecond
          notificationService.notify(s"${pos.positionType.toString} - Close (open at ${pos.openedAt}, close at $lastPrice, opened for $openTime s)")
          positionsService.close(pos.id.get, name, lastPrice)
          position = None
        }
        case None => strategy.open(lastTime) foreach { positionType =>
          notificationService.notify(s"${positionType.toString} - Open ($lastPrice)")
          positionsService.open(name, lastPrice, 1)
              .map(posId => InitPosition(Some(Position(
                Some(posId), name, 1, positionType, Instant.ofEpochSecond(lastTime), lastPrice, None, None
              )))) pipeTo self
        }
      }
  }
}

object TradeAdvisorBotActor {
  case class DoAnalyze(historyRecords: Seq[Trade])
  case class InitPosition(position: Option[Position])
}