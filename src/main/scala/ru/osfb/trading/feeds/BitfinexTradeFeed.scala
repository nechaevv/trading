package ru.osfb.trading.feeds

import java.time.Instant

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import play.api.libs.json.Json
import ru.osfb.trading.db.TradeRecord
import ru.osfb.webapi.core.{ActorMaterializerComponent, ActorSystemComponent, ExecutionContextComponent}
import ru.osfb.webapi.http.PlayJsonMarshallers._
import scala.concurrent.Future

/**
  * Created by v.a.nechaev on 19.04.2016.
  */
class BitfinexTradeFeed(val symbol: String) extends TradeFeed{ this: ActorSystemComponent with ActorMaterializerComponent with ExecutionContextComponent =>
  override def exchange: String = "bitfinex"
  case class BitfinexTrade(timestamp: Long, tid: Long, price: String, amount: String, `type`: String)
  implicit val tradeReads = Json.reads[BitfinexTrade]

  override def poll(from: Long): Future[Seq[TradeRecord]] = {
    Http().singleRequest(HttpRequest(uri = s"https://api.bitfinex.com/v1/trades/$symbol?timestamp=$from"))
      .flatMap(resp => Unmarshal(resp.entity).to[Seq[BitfinexTrade]])
      .map(_.map(t => TradeRecord(exchange, symbol, t.tid.toString, Instant.ofEpochSecond(t.timestamp), BigDecimal(t.price), BigDecimal(t.amount))))
  }

}
