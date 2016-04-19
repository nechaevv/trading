package ru.osfb.trading.connectors

import java.time.Instant

import ru.osfb.trading.DatabaseComponent
import ru.osfb.trading.calculations.Trade
import ru.osfb.trading.db.TradeHistoryModel.TradeHistoryTable
import ru.osfb.trading.db.TradeRecord
import ru.osfb.webapi.core.ExecutionContextComponent
import slick.dbio.DBIOAction

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by sgl on 09.04.16.
  */
trait DbHistoryServiceComponentImpl extends TradeHistoryServiceComponent { this: DatabaseComponent with ExecutionContextComponent =>
  class TradeHistoryServiceImpl extends TradeHistoryService {
    import ru.osfb.trading.PgDriver.api._
    import ru.osfb.trading.db.TradeHistoryModel.tradeHistoryTable

    override def loadHistory(exchange: String, symbol: String, from: Long, till: Long): Future[Seq[Trade]] = database run runQuery {
      tradeHistoryTable.filter(t => t.exchange === exchange && t.symbol === symbol && t.time.between(Instant.ofEpochSecond(from),Instant.ofEpochSecond(till)))
    }

    override def loadLatest(exchange: String, symbol: String, depth: Long): Future[Seq[Trade]] = database run {
      val q = tradeHistoryTable.filter(t => t.exchange === exchange && t.symbol === symbol)
      q.map(_.time).max.result.flatMap {
        case Some(maxTime) => runQuery(q.filter(_.time >= maxTime.minusSeconds(depth)))
        case None => DBIOAction.successful(Nil)
      }
    }

    protected def runQuery(q: Query[TradeHistoryTable, TradeRecord, Seq]) = q.sortBy(_.time).result
      .map(_.map(th => Trade(th.time.toEpochMilli/1000, (th.price * th.quantity).toDouble, th.quantity.toDouble)))

  }
}
