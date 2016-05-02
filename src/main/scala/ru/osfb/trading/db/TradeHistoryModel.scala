package ru.osfb.trading.db

import java.time.Instant

import ru.osfb.trading.db.TradeType.TradeType

/**
  * Created by sgl on 02.04.16.
  */
object TradeHistoryModel {
  import ru.osfb.trading.PgDriver.api._
  implicit val tradeTypeColumnType = MappedColumnType.base[TradeType, String](
    {tt => tt.toString}, {s => TradeType.withName(s)}
  )
  class TradeHistoryTable(t:Tag) extends Table[TradeRecord](t, "TRADE_HISTORY") {
    def exchange = column[String]("EXCHANGE")
    def symbol = column[String]("SYMBOL")
    def id = column[String]("TRADE_ID")
    def time = column[Instant]("TIME")
    def price = column[BigDecimal]("PRICE")
    def quantity = column[BigDecimal]("QUANTITY")
    def tradeType = column[TradeType]("TRADE_TYPE")
    def * = (exchange, symbol, id, time, price, quantity, tradeType) <> (TradeRecord.tupled, TradeRecord.unapply)
    def idx = index("TRADE_HISTORY_IDX", (exchange, symbol, time), unique = false)
    def pk = primaryKey("TRADE_HISTORY_PK", (exchange, symbol, id))
  }
  val tradeHistoryTable = TableQuery[TradeHistoryTable]
}

case class TradeRecord
(
  exchange: String,
  symbol: String,
  id: String,
  time: Instant,
  price: BigDecimal,
  quantity: BigDecimal,
  tradeType: TradeType
)

object TradeType extends Enumeration {
  type TradeType = Value
  val Buy = Value("B")
  val Sell = Value("S")
}