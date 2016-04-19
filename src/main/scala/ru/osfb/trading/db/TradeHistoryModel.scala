package ru.osfb.trading.db

import java.time.Instant

/**
  * Created by sgl on 02.04.16.
  */
object TradeHistoryModel {
  import ru.osfb.trading.PgDriver.api._
  class TradeHistoryTable(t:Tag) extends Table[TradeRecord](t, "TRADE_HISTORY") {
    def exchange = column[String]("EXCHANGE")
    def symbol = column[String]("SYMBOL")
    def id = column[String]("ID")
    def time = column[Instant]("TIME")
    def price = column[BigDecimal]("PRICE")
    def quantity = column[BigDecimal]("QUANTITY")
    def * = (exchange, symbol, id, time, price, quantity) <> (TradeRecord.tupled, TradeRecord.unapply)
    def idx = index("TRADE_HISTORY_IDX", (exchange, symbol, time), unique = false)
    def pk = primaryKey("TRADE_HISTORY_PK", (exchange, symbol, id))
  }
  val tradeHistoryTable = TableQuery[TradeHistoryTable]
}

case class TradeRecord(exchange: String, symbol: String, id: String, time: Instant, price: BigDecimal, quantity: BigDecimal)
