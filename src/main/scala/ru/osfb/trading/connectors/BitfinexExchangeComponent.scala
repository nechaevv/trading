package ru.osfb.trading.connectors

import java.math.BigInteger
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json._
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

    protected val apiUrl = "https://api.bitfinex.com"
    import ru.osfb.trading.connectors.BitfinexProtocol._

    def tradeHistory(symbol: String, from: Long, limitOpt: Option[Int] = None): Future[Seq[Trade]] = Http()
      .singleRequest(HttpRequest(
        uri = apiUrl + "/v1/trades/" + symbol + "?timestamp=" + from + limitOpt.fold("")(lim => "&limit_trades=" + lim),
        method = HttpMethods.GET
      )).flatMap(resp => Unmarshal(resp.entity).to[Seq[Trade]]).withErrorLog(logger)

    def balances = authRequest[Seq[Balance]]("/v1/balances", Json.obj())

    def newOrder(symbol: String, side: String, orderType: String, amount: BigDecimal, price: BigDecimal) = {
      authRequest[OrderStatus]( "/v1/order/new", Json.obj("symbol" -> symbol, "amount" -> amount, "price" -> price,
        "exchange" -> "bitfinex", "side" -> side, "type" -> orderType))
    }

    def orderStatus(orderId: Long) = authRequest[OrderStatus]("/v1/order/status", Json.obj("order_id" -> orderId))

    def cancelOrder(orderId: Long) = authRequest[OrderStatus]("/v1/order/cancel", Json.obj("order_id" -> orderId))

    private lazy val apiKey = configuration.getString("bitfinex.api-key")
    private lazy val apiSecret = new SecretKeySpec(
      configuration.getString("bitfinex.api-secret").getBytes, "HmacSHA384")

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

