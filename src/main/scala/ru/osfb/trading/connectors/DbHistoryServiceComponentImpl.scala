package ru.osfb.trading.connectors

import java.time.Instant

import ru.osfb.trading.DatabaseComponent
import ru.osfb.trading.calculations.Trade
import ru.osfb.webapi.core.ExecutionContextComponent

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by sgl on 09.04.16.
  */
trait DbHistoryServiceComponentImpl extends TradeHistoryServiceComponent { this: DatabaseComponent with ExecutionContextComponent =>
  class TradeHistoryServiceImpl extends TradeHistoryService {
    import ru.osfb.trading.PgDriver.api._
    import ru.osfb.trading.db.TradeHistoryModel.tradeHistoryTable
    override def loadHistory(exchange: String, symbol: String, from: Long, tillOpt: Option[Long]): Future[Seq[Trade]] = database run {
      tradeHistoryTable.filter(t =>
        tillOpt.fold(t.time > Instant.ofEpochSecond(from))(till => t.time.between(Instant.ofEpochSecond(from),Instant.ofEpochSecond(till)))
      ).filter(t => t.exchange === exchange && t.symbol === symbol)
        .sortBy(_.time).result
        .map(_.map(th => Trade(th.time.toEpochMilli/1000, (th.price * th.quantity).toDouble, th.quantity.toDouble)))
    }
  }
}
