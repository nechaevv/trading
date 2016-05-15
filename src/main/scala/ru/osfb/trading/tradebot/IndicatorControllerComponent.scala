package ru.osfb.trading.tradebot

import java.time.Instant

import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import ru.osfb.trading.calculations.ArrayTradeHistory
import ru.osfb.trading.connectors.{BitfinexExchangeComponent, TradeHistoryServiceComponent}
import ru.osfb.trading.strategies.TrendIndicators
import ru.osfb.webapi.core.ExecutionContextComponent
import ru.osfb.webapi.http.PlayJsonMarshallers._
import ru.osfb.webapi.utils.FutureUtils._
/**
  * Created by sgl on 09.04.16.
  */
trait IndicatorControllerComponent {
    this: ExecutionContextComponent with BitfinexExchangeComponent with TradeHistoryServiceComponent =>
  implicit val indicatorWrites = Json.writes[TrendIndicators]
  def indicatorController = path("indicators" / Segment) { symbol =>
    onSuccess({
      val now = Instant.now().toEpochMilli / 1000
      tradeHistoryService.loadHistory("bitfinex", symbol, now - 5*TradeBot.strategy.timeFrame, now) map { trades =>
        implicit val tradeHistory = new ArrayTradeHistory(trades)
        TradeBot.strategy.indicators(now)
      }
    } withErrorLog logger) {
      complete(_)
    }
  }

  private lazy val logger = Logger(LoggerFactory.getLogger(classOf[IndicatorControllerComponent]))

}
