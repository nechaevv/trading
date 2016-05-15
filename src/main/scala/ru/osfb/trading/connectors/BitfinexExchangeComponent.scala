package ru.osfb.trading.connectors

import java.math.BigInteger
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json._
import ru.osfb.trading.calculations.Trade
import ru.osfb.webapi.core.{ActorMaterializerComponent, ActorSystemComponent, ConfigurationComponent, ExecutionContextComponent}
import ru.osfb.webapi.http.PlayJsonMarshallers._
import ru.osfb.webapi.utils.FutureUtils._
import ru.osfb.webapi.utils.Hex

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

    protected val apiUrl = "https://api.bitfinex.com"

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

    implicit val walletBalancesReads = {
      //implicit val balanceReads =
        Json.reads[WalletBalance]
      //Json.reads[Seq[WalletBalance]]
    }
    def balances = authRequest[Seq[WalletBalance]]("/v1/balances", Json.obj())

    private lazy val apiKey = configuration.getString("bitfinex.api-key")
    private lazy val apiSecret = new SecretKeySpec(
      configuration.getString("bitfinex.api-secret").getBytes, "HmacSHA384")

    protected def fetchTrades(symbol: String, from: Long): Future[Seq[BitfinexTrade]] = {
      logger.trace(s"Fetching trades for $symbol from $from (${Instant.ofEpochSecond(from)})")
      val req = HttpRequest(uri = apiUrl + "/v1/trades/" + symbol + "?timestamp=" + from, method = HttpMethods.GET)
      //logger.trace(req.toString)
      Http().singleRequest(req)
        .flatMap(resp => Unmarshal(resp.entity).to[Seq[BitfinexTrade]])
    }

    protected def authRequest[Res](method: String, request: JsValue)(implicit resR: Reads[Res]): Future[Res] = {
      val mac = Mac.getInstance("HmacSHA384")
      val reqJson = Json.stringify(request.asInstanceOf[JsObject] ++ Json.obj("nonce" -> System.currentTimeMillis().toString, "request" -> method))
      val payload = Base64.getEncoder.encodeToString(reqJson.getBytes)
      mac.init(apiSecret)
      mac.update(payload.getBytes)
      val signature = String.format("%096x", new BigInteger(1, mac.doFinal()))
      val reqHeaders:List[HttpHeader] = List(
        RawHeader("X-BFX-APIKEY", apiKey),
        RawHeader("X-BFX-PAYLOAD", payload),
        RawHeader("X-BFX-SIGNATURE", signature)
      )
      val httpRequest = HttpRequest(
        method = HttpMethods.POST, uri = apiUrl + method, headers = reqHeaders,
        entity = HttpEntity.Strict(ContentType(MediaTypes.`application/json`), ByteString(payload.getBytes))
      )
      logger.trace(httpRequest.toString)
      for {
        response <- Http().singleRequest(httpRequest)
        result <- Unmarshal(response).to[Res]
      } yield result
    }

  }

}

case class WalletBalance(`type`: String, currency: String, amount: BigDecimal, available: BigDecimal)
