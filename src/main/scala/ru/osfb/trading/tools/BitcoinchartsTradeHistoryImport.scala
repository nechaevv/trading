package ru.osfb.trading.tools

import java.time.Instant

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.db.{TradeRecord, TradeType}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import ru.osfb.webapi.utils.FutureUtils._
import ru.osfb.trading.db.TradeHistoryModel.tradeHistoryTable

/**
  * Created by sgl on 02.04.16.
  */
object BitcoinchartsTradeHistoryImport extends App with LazyLogging {
  val batchSize = 100000
  val symbol = args(0)
  for {
    batch <- Source.fromFile(s"$symbol.csv").getLines().grouped(batchSize)
  } {
    import ru.osfb.trading.TradingConfiguration.database
    val trades = batch map { line => line.split(",") match {
      case Array(unixTimeStr, priceStr, qtyStr) => TradeRecord("bitcoincharts", symbol, "", Instant.ofEpochSecond(unixTimeStr.toLong), BigDecimal(priceStr), BigDecimal(qtyStr), TradeType.Sell)
    } }
    import ru.osfb.trading.PgDriver.api._
    Await.ready((database run {
      tradeHistoryTable ++= trades
    }.transactionally).withErrorLog(logger), 1.minute)
    logger.trace(s"${trades.length} history records loaded" + trades.lastOption.fold("")(lastTrade => s"(up to ${lastTrade.time})"))
  }
}
