package ru.osfb.trading.connectors

import java.time.Instant

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.Trade
import ru.osfb.webapi.core.{ActorMaterializerComponent, ActorSystemComponent, ExecutionContextComponent}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

/**
 * Created by: sgl
 * Date: 12.12.13
 */
trait BitcoinchartsServiceComponent {
  this: ActorSystemComponent
  with ActorMaterializerComponent
  with ExecutionContextComponent =>

  class BitcoinchartsService extends LazyLogging {

    def fetchAllTrades(symbol: String, from: Long, till: Long): Future[Seq[Trade]] = {
      def fetchAndAppend(symbol: String, fromNext: Long, acc: Seq[Trade]): Future[Seq[Trade]] = loadHistoryFromHttp(symbol, fromNext)
        .flatMap(trades => if (trades.isEmpty || trades.last.time >= till) Future.successful(acc ++ trades)
        else fetchAndAppend(symbol, trades.last.time + 1, acc ++ trades))
      val r = fetchAndAppend(symbol, from, Nil)
      for (trades <- r) logger.trace(s"Load ${trades.length} trades for $symbol" + (if(trades.isEmpty) "" else
          s" from ${Instant.ofEpochSecond(trades.head.time)} till ${Instant.ofEpochSecond(trades.last.time)}"))
      r
    }

    def loadHistoryFromHttp(symbol: String, from: Long): Future[Seq[Trade]] = {
      logger.debug(s"Fetching trades for $symbol from $from (${Instant.ofEpochSecond(from)})")
      Http().singleRequest(HttpRequest(uri = s"http://api.bitcoincharts.com/v1/trades.csv?symbol=$symbol&start=$from", method = HttpMethods.GET))
        .flatMap(response => implicitly[Unmarshaller[HttpEntity, String]].apply(response.entity))
        .map(result => parseHistoryCsv(Source.fromString(result)))
    }

    def loadHistoryFromCsvFile(file: String) = {
      logger.info("Loading history from " + file)
      parseHistoryCsv(Source.fromFile(file))
    }

    private def parseHistoryCsv(source: Source) = source.getLines().map(line => line.split(",") match {
      case Array(timeStr, priceStr, qtyStr) => Trade(timeStr.toLong, priceStr.toDouble * qtyStr.toDouble, qtyStr.toDouble)
    }).toSeq

  }

  def bitcoinchartsService: BitcoinchartsService

}
