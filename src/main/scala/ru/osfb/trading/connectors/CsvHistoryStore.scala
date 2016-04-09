package ru.osfb.trading.connectors

import java.time.Instant

import ru.osfb.trading.calculations.Trade

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

/**
  * Created by sgl on 09.04.16.
  */
object CsvHistoryStore {
  def loadHistory(symbol: String, from: Instant, till: Instant)(implicit executionContext: ExecutionContext): Future[Array[Trade]] = Future {
    val fromSec = from.toEpochMilli / 1000
    val tillSec = till.toEpochMilli / 1000
    Source.fromFile(symbol+".csv").getLines().filter(line => {
      val time = line.takeWhile(_ != ',').toLong
      time >= fromSec && time <= tillSec
    }).map(line => line.split(",") match {
      case Array(timeStr, priceStr, qtyStr) => Trade(timeStr.toLong, priceStr.toDouble * qtyStr.toDouble, qtyStr.toDouble)
    }).toArray
  }
}
