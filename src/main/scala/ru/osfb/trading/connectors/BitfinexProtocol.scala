package ru.osfb.trading.connectors

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Created by sgl on 15.05.16.
  */
object BitfinexProtocol {
  case class Trade(timestamp: Long, tid: Long, `type`: String, price: BigDecimal, amount: BigDecimal)
  case class Balance(`type`: String, currency: String, amount: BigDecimal, available: BigDecimal)
  case class OrderStatus(id: Long, symbol: String, price: BigDecimal, averagePrice: BigDecimal, side: String, orderType: String,
                         timestamp: Long, isActive: Boolean, isCancelled: Boolean)

  implicit val tradeReads = Json.reads[Trade]
  implicit val walletBalancesReads = Json.reads[Balance]
  implicit val orderStatusReads = (
    (__ \ "id").read[Long] ~
      (__ \ "symbol").read[String] ~
      (__ \ "price").read[BigDecimal] ~
      (__ \ "avg_execution_price").read[BigDecimal] ~
      (__ \ "side").read[String] ~
      (__ \ "type").read[String] ~
      (__ \ "timestamp").read[Long] ~
      (__ \ "is_live").read[Boolean] ~
      (__ \ "is_cancelled").read[Boolean]
    )(OrderStatus)

}
