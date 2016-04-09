package ru.osfb.trading.db

import java.time.Instant

/**
  * Created by sgl on 02.04.16.
  */
object TradeHistoryModel {
  import ru.osfb.trading.PgDriver.api._
  class TradeHistoryTable(t:Tag) extends Table[TradeRecord](t, "TRADE_HISTORY") {
    def symbol = column[String]("SYMBOL")
    def time = column[Instant]("TIME")
    def price = column[BigDecimal]("PRICE")
    def quantity = column[BigDecimal]("QUANTITY")
    def * = (symbol, time, price, quantity) <> (TradeRecord.tupled, TradeRecord.unapply)
    def idx = index("TRADE_HISTORY_IDX", (symbol, time), unique = false)
  }
  val tradeHistoryTable = TableQuery[TradeHistoryTable]
}

case class TradeRecord(symbol: String, time: Instant, price: BigDecimal, quantity: BigDecimal)
