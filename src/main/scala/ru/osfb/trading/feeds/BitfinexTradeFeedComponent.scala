package ru.osfb.trading.feeds

import java.time.Instant

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import play.api.libs.json.Json
import ru.osfb.trading.connectors.BitfinexExchangeComponent
import ru.osfb.trading.db.{TradeRecord, TradeType}
import ru.osfb.webapi.core.{ActorMaterializerComponent, ActorSystemComponent, ExecutionContextComponent}
import ru.osfb.webapi.http.PlayJsonMarshallers._

import scala.concurrent.Future

/**
  * Created by v.a.nechaev on 19.04.2016.
  */
trait BitfinexTradeFeedComponent {
  this: ActorSystemComponent with ActorMaterializerComponent with ExecutionContextComponent
  with BitfinexExchangeComponent =>

  class BitfinexTradeFeed(val symbol: String) extends TradeFeed {
    override def exchange: String = "bitfinex"

    override def poll(from: Long): Future[Seq[TradeRecord]] = {
      bitfinexExchange.tradeHistory(symbol, from).map(_.map(t => {
          val tt = t.`type` match {
            case "sell" => TradeType.Sell
            case "buy" => TradeType.Buy
          }
          TradeRecord(exchange, symbol, t.tid.toString, Instant.ofEpochSecond(t.timestamp), t.price, t.amount, tt)
        }))
    }
  }

}