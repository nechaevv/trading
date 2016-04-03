package ru.osfb.trading.indicators

import java.time.{Duration, Instant}

import ru.osfb.trading.model.TradeHistoryModel
import slick.dbio.Effect.Read
import slick.profile.FixedSqlAction
import ru.osfb.trading.PgDriver.api._
import TradeHistoryModel.tradeHistoryTable

/**
  * Created by sgl on 02.04.16.
  */
class ExponentialMovingAverage(timeFrame: Duration) extends MomentIndicator {
  override def apply(symbol: String, at: Instant): FixedSqlAction[Option[BigDecimal], NoStream, Read] = {
    val from = at minus (timeFrame multipliedBy 5) //0.99326 of infinite domain
    val timeFrameSeconds = BigDecimal(timeFrame.getSeconds)
    val q = tradeHistoryTable.filter(t => (t.symbol === symbol) && (t.time between(from, at)))
    val sum = q.map(t => t.price * t.quantity / exp(intervalSeconds(valueToConstColumn(at) - t.time) / timeFrameSeconds)).sum
    val qty = q.map(t => t.quantity / exp(intervalSeconds(valueToConstColumn(at) - t.time) / timeFrameSeconds)).sum
    (sum / qty).result
  }
}
