package ru.osfb.trading.connectors

import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{JsValue, Json}
import ru.osfb.trading.calculations.Trade
import ru.osfb.webapi.core.{ActorMaterializerComponent, ActorSystemComponent, ConfigurationComponent, ExecutionContextComponent}
import ru.osfb.webapi.http.PlayJsonMarshallers._
import ru.osfb.webapi.utils.FutureUtils._

import scala.concurrent.Future

/**
  * Created by sgl on 09.04.16.
  */
trait BitfinexExchangeComponent {
  this: ActorSystemComponent
    with ActorMaterializerComponent
    with ExecutionContextComponent
    with ConfigurationComponent =>

  def bitfinexExchange: BitfinexExchange

  class BitfinexExchange extends LazyLogging {

    protected val apiUrl = "https://api.bitfinex.com/v1"

    case class BitfinexTrade(timestamp: Long, price: String, amount: String)

    implicit val tradeReads = Json.reads[BitfinexTrade]

    def fetchTradeHistory(symbol: String, from: Long, till: Long): Future[Seq[Trade]] = {
      def fetchNext(fromNext: Long, acc: Seq[BitfinexTrade]): Future[Seq[BitfinexTrade]] = {
        fetchTrades(symbol, fromNext).map({ trades =>
          //logger.trace("Received trades: " + trades.toString())
          trades.reverse
        }).flatMap(trades => if (trades.isEmpty || trades.last.timestamp >= till) Future.successful(acc ++ trades)
          else fetchNext(trades.last.timestamp + 1, acc ++ trades))
      }
      fetchNext(from, Nil).map(trades => {
        logger.trace(s"Load ${trades.length} trades for $symbol" + (if(trades.isEmpty) "" else
        s" from ${Instant.ofEpochSecond(trades.head.timestamp)} till ${Instant.ofEpochSecond(trades.last.timestamp)}"))
        trades.map {
          case BitfinexTrade(timestamp, priceStr, quantityStr) => Trade(timestamp, priceStr.toDouble * quantityStr.toDouble, quantityStr.toDouble)
        }
      })
    }.withErrorLog(logger)

    private lazy val apiKey = configuration.getString("bitfinex.api-key")
    private lazy val apiSecret = new SecretKeySpec(
      configuration.getString("bitfinex.api-secret").getBytes, "HmacSHA384")
    private def mac = Mac.getInstance("HmacSHA384")

    protected def fetchTrades(symbol: String, from: Long): Future[Seq[BitfinexTrade]] = {
      logger.trace(s"Fetching trades for $symbol from $from (${Instant.ofEpochSecond(from)})")
      val req = HttpRequest(uri = apiUrl + "/trades/" + symbol + "?timestamp=" + from, method = HttpMethods.GET)
      //logger.trace(req.toString)
      Http().singleRequest(req)
        .flatMap(resp => Unmarshal(resp.entity).to[Seq[BitfinexTrade]])
    }

    protected def authHeaders(payload: String): Seq[HttpHeader] = {
      mac.init(apiSecret)
      ???
    }

  }
}