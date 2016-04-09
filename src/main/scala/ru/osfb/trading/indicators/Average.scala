package ru.osfb.trading.indicators

import java.time.Instant

import ru.osfb.trading.db.TradeHistoryModel
import slick.dbio.Effect.Read
import slick.profile.FixedSqlAction

/**
  * Created by sgl on 02.04.16.
  */
object Average extends IntervalIndicator {
  import TradeHistoryModel.tradeHistoryTable
  import ru.osfb.trading.PgDriver.api._

  override def apply(symbol: String, from: Instant, till: Instant): FixedSqlAction[Option[BigDecimal], NoStream, Read] = {
    val q = tradeHistoryTable.filter(t => (t.symbol === symbol) && (t.time between(from, till)))
      //.map(t => ((t.price * t.quantity).sum, t.quantity.sum))
    val sum = q.map(t => t.price * t.quantity).sum
    val qty = q.map(_.quantity).sum
    (sum / qty).result
  }
}
