package ru.osfb.trading.connectors

import java.time.Instant

import ru.osfb.trading.calculations.Trade

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by sgl on 09.04.16.
  */
object DatabaseHistoryStore {
  def loadHistory(symbol: String, from: Instant, till: Instant)(implicit executionContext: ExecutionContext): Future[Iterable[Trade]] = {
    import ru.osfb.trading.TradingConfiguration.database
    import ru.osfb.trading.PgDriver.api._
    import ru.osfb.trading.db.TradeHistoryModel.tradeHistoryTable
    database run {
      tradeHistoryTable
        .filter(t => t.symbol === symbol && t.time.between(from,till))
          .result.map(_.map(th => Trade(th.time.toEpochMilli/1000, (th.price * th.quantity).toDouble, th.quantity.toDouble)))
    }
  }
}
