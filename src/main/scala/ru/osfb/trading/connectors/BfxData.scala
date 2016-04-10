package ru.osfb.trading.connectors

import java.time.Instant

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.Trade

import scala.io.Source

/**
  * Created by sgl on 10.04.16.
  */
object BfxData extends LazyLogging {
  def loadVwapData(symbol:String, from: Long, till: Long): Seq[Trade] = {
    val fromStr = from.toString
    val tillStr = till.toString
    val trades = Source.fromFile(s"vwapHourly$symbol.csv").getLines().drop(1)
      .filter({ line =>
        val time = line.takeWhile(_ != ",")
        (time >= fromStr) && (time <= tillStr)
      }).map(_.split(",") match {
      case Array(timeStr, _, priceStr, volumeStr) => Trade(timeStr.toLong, priceStr.toDouble * volumeStr.toDouble, volumeStr.toDouble)
    }).toSeq.reverse
    logger.info(s"Loaded ${trades.length} vwap records as trades" +
      (if (trades.isEmpty) "" else s"from ${trades.head.time} (${Instant.ofEpochSecond(trades.head.time)})" +
      s" till ${trades.head.time} (${Instant.ofEpochSecond(trades.head.time)})"))
    trades
  }
}
