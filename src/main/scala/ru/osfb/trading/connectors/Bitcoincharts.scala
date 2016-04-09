package ru.osfb.trading.connectors

import java.time.Instant

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations.Trade

import scala.concurrent.ExecutionContext
import scala.io.Source

/**
 * Created by: sgl
 * Date: 12.12.13
 */
object Bitcoincharts extends LazyLogging {

//  def loadHistoryFromHttp(ticker: String, since: Long)(implicit ec: ExecutionContext) = {
//    logger.debug(s"Loading trade history for $ticker from ${Instant.ofEpochSecond(since)}")
//    Http().singleRequest(HttpRequest(uri = s"http://api.bitcoincharts.com/v1/trades.csv?symbol=$ticker&start=$since", method = HttpMethods.GET))
//      .flatMap(response => implicitly[Unmarshaller[HttpEntity, String]].apply(response.entity))
//        .map(result => parseHistoryCsv(Source.fromString(result)))
//  }
  
  def loadHistoryFromCsvFile(file: String) = {
    logger.info("Loading history from " + file)
    parseHistoryCsv(Source.fromFile(file))
  }

  private def parseHistoryCsv(source: Source) = source.getLines().map(line => line.split(",") match {
    case Array(timeStr, priceStr, qtyStr) => Trade(timeStr.toLong, priceStr.toDouble * qtyStr.toDouble, qtyStr.toDouble)
  }).toIterable

}
