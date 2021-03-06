package ru.osfb.trading.feeds

import akka.actor.Actor
import akka.pattern.pipe
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.PgDriver.api._
import ru.osfb.trading.db.TradeHistoryModel.tradeHistoryTable
import ru.osfb.trading.db.TradeRecord
import ru.osfb.trading.notification.NotificationService
import ru.osfb.webapi.utils.FutureUtils._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success}

/**
  * Created by v.a.nechaev on 19.04.2016.
  */
class TradeHistoryDownloaderActor (
  feed: TradeFeed,
  pollInterval: FiniteDuration,
  database: Database,
  notificationService: NotificationService
) extends Actor with LazyLogging {
  implicit def executionContext = context.dispatcher

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    database run {
      tradeHistoryTable.filter(t => t.exchange === feed.exchange && t.symbol === feed.symbol).map(_.time).max.result
    } foreach { maxTimeOpt =>
      val maxTime = maxTimeOpt.fold(0L)(_.getEpochSecond)
      logger.info(s"Starting trade downloader for ${feed.exchange} - ${feed.symbol} from $maxTime")
      self ! LoadCompleted(maxTime)
      context.system.scheduler.schedule(Duration.Zero, pollInterval, self, Poll)
    }
  }

  var from: Long = 0

  override def receive: Receive = {
    case Poll => feed.poll(from) pipeTo self
    case trades: Seq[TradeRecord] => if (trades.nonEmpty) {
      database run {
        tradeHistoryTable.filter(t => t.exchange === feed.exchange && t.symbol === feed.symbol && t.id.inSet(trades.map(_.id)))
          .delete andThen (tradeHistoryTable ++= trades).transactionally
      } withErrorLog logger onComplete {
        case Success(Some(result)) =>
          logger.info(s"Updated $result trade history records for ${feed.symbol} from ${feed.exchange}")
          HistoryUpdateEventBus.publish(HistoryUpdateEvent(feed.exchange, feed.symbol))
          self ! LoadCompleted(trades.head.time.toEpochMilli / 1000)
        case Success(_) => ()
        case Failure(ex) => notificationService.notify(s"Poll failed for ${feed.exchange} - ${feed.symbol}: " + ex.getMessage)
      }
    }
    case LoadCompleted(lastTime) => from = Math.max(from, lastTime)

  }
}

case class LoadCompleted(lastTime: Long)
case object Poll