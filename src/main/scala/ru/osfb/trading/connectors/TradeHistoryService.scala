package ru.osfb.trading.connectors

import ru.osfb.trading.calculations.Trade

/**
  * Created by v.a.nechaev on 19.04.2016.
  */
trait TradeHistoryService {
  def loadHistory(exchange: String, symbol: String, from: Long, till: Option[Long]): Seq[Trade]
}

trait TradeHistoryServiceComponent {
  def tradeHistoryService: TradeHistoryService
}
