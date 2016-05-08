package ru.osfb.trading.feeds

import java.time.Instant

import akka.actor.Actor
import akka.pattern.pipe
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.PgDriver.api._
import ru.osfb.trading.db.TradeHistoryModel.tradeHistoryTable
import ru.osfb.trading.db.TradeRecord
import ru.osfb.webapi.utils.FutureUtils._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Success

/**
  * Created by v.a.nechaev on 19.04.2016.
  */
class TradeHistoryDownloaderActor (
  feed: TradeFeed,
  pollInterval: FiniteDuration,
  database: Database
) extends Actor with LazyLogging {
  implicit def executionContext = context.dispatcher

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context.system.scheduler.schedule(Duration.Zero, pollInterval, self, Poll)
  }

  var from: Long = 0

  override def receive: Receive = {
    case Poll => feed.poll(from) pipeTo self
    case trades:                                                                                                                                                                                                                                                                  Seq[TradeRecord] => if (trades.nonEmpty) {
      val historyQuery = tradeHistoryTable.filter(t => t.exchange === feed.exchange && t.symbol === feed.symbol)
      database run {
        historyQuery.filter(t => t.time between (trades.last.time, trades.head.time)).map(_.id).result
          .flatMap(ids => {
            val idSet = ids.toSet
            tradeHistoryTable ++= trades.filter(t => !(idSet contains t.id))
          }
        ).transactionally
      } withErrorLog logger onComplete {
        case Success(Some(result)) =>
          logger.info(s"Updated $result trade history records for ${feed.symbol} from ${feed.exchange}")
          HistoryUpdateEventBus.publish(HistoryUpdateEvent(feed.exchange, feed.symbol))
          self ! LoadCompleted(trades.head.time.toEpochMilli / 1000)
        case _ => ()
      }
    }
    case LoadCompleted(lastTime) => from = Math.max(from, lastTime)

  }
}

case class LoadCompleted(lastTime: Long)
case object Poll