package ru.osfb.trading.connectors

import ru.osfb.trading.calculations.Trade

import scala.concurrent.Future

/**
  * Created by v.a.nechaev on 19.04.2016.
  */
trait TradeHistoryService {
  def loadHistory(exchange: String, symbol: String, from: Long, till: Long): Future[Seq[Trade]]
  def loadLatest(exchange:String, symbol: String, depth: Long): Future[Seq[Trade]]
}

trait TradeHistoryServiceComponent {
  def tradeHistoryService: TradeHistoryService
}
