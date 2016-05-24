package ru.osfb.trading.connectors

import play.api.libs.json._
import play.api.libs.functional.syntax._
import ru.osfb.trading.connectors.BitfinexProtocol.OrderSide.OrderSide
import ru.osfb.trading.connectors.BitfinexProtocol.OrderType.OrderType
import ru.osfb.trading.connectors.BitfinexProtocol.WalletType.WalletType

/**
  * Created by sgl on 15.05.16.
  */
object BitfinexProtocol {
  case class Trade(timestamp: Long, tid: Long, `type`: OrderSide, price: BigDecimal, amount: BigDecimal)
  case class Balance(`type`: WalletType, currency: String, amount: BigDecimal, available: BigDecimal)
  case class OrderBookEntry(price: BigDecimal, amount: BigDecimal, timestamp: Long)
  case class OrderBook(bids: Seq[OrderBookEntry], asks: Seq[OrderBookEntry])
  case class OrderStatus(id: Long, symbol: String, price: BigDecimal, averagePrice: BigDecimal, amount: BigDecimal,
                         side: OrderSide, orderType: OrderType, timestamp: Long, isActive: Boolean, isCancelled: Boolean)

  object OrderSide extends Enumeration {
    type OrderSide = Value
    val Buy = Value("buy")
    val Sell = Value("sell")
  }

  object WalletType extends Enumeration {
    type WalletType = Value
    val Exchange = Value("exchange")
    val Trading = Value("trading")
    val Deposit = Value("deposit")
  }

  object OrderType extends Enumeration {
    type OrderType = Value
    val Market = Value("market")
    val Limit = Value("limit")
    val Stop = Value ("stop")
    val TrailingStop = Value("trailing-stop")
    val FillOrKill = Value("fill-or-kill")
    val ExchangeMarket = Value("exchange market")
    val ExchangeLimit = Value("exchange limit")
    val ExchangeStop = Value ("exchange stop")
    val ExchangeTrailingStop = Value("exchange trailing-stop")
    val ExchangeFillOrKill = Value("exchange fill-or-kill")
  }

  implicit val tradeReads = Json.reads[Trade]
  implicit val orderBookReads = Json.reads[OrderBook]
  implicit val walletBalancesReads = Json.reads[Balance]
  implicit val orderStatusReads = (
    (__ \ "id").read[Long] ~
      (__ \ "symbol").read[String] ~
      (__ \ "price").read[BigDecimal] ~
      (__ \ "avg_execution_price").read[BigDecimal] ~
      (__ \ "executed_amount").read[BigDecimal] ~
      (__ \ "side").read[OrderSide] ~
      (__ \ "type").read[OrderType] ~
      (__ \ "timestamp").read[Long] ~
      (__ \ "is_live").read[Boolean] ~
      (__ \ "is_cancelled").read[Boolean]
    )(OrderStatus)

  implicit val orderSideFormat = enumFormat(OrderSide)
  implicit val walletTypeFormat = enumFormat(WalletType)
  implicit val orderTypeFormat = enumFormat(OrderType)

  def enumFormat[T <: Enumeration](enum: T) = Format(Reads[enum.Value]({
    case JsString(s) => enum.values.find(_.toString == s).fold[JsResult[enum.Value]](JsError())(JsSuccess(_))
    case _ => JsError()
  }), Writes[T](s => JsString(s.toString)))

}
