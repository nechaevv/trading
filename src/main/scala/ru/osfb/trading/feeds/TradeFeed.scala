package ru.osfb.trading.feeds

import ru.osfb.trading.db.TradeRecord

import scala.concurrent.Future

/**
  * Created by v.a.nechaev on 19.04.2016.
  */
trait TradeFeed {
  def exchange: String
  def symbol: String
  def poll(from: Long): Future[Seq[TradeRecord]]
}
