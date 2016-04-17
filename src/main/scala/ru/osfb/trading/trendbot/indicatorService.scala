package ru.osfb.trading.trendbot

import java.time.Instant
import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.calculations._
import ru.osfb.trading.connectors.{BfxData, BitcoinchartsServiceComponent, BitfinexExchangeComponent}
import ru.osfb.webapi.core.{ConfigurationComponent, ExecutionContextComponent}

import scala.concurrent.Future

/**
  * Created by sgl on 10.04.16.
  */

case class Indicators(price: Double, trendFactor: Double)

trait IndicatorService {
  def computeIndicators(symbol: String): Future[Indicators]
}

trait IndicatorServiceComponent {   this: BitfinexExchangeComponent with BitcoinchartsServiceComponent
  with ConfigurationComponent with ExecutionContextComponent =>

  class IndicatorServiceImpl extends IndicatorService with LazyLogging {
    lazy val avgTimeFrame = configuration.getDuration("trend-factor.avg-time-frame", TimeUnit.SECONDS)
    lazy val trendTimeSpan = configuration.getDuration("trend-factor.trend-time-span", TimeUnit.SECONDS)

    def computeIndicators(symbol: String): Future[Indicators] = {
      val till = System.currentTimeMillis() / 1000
      val from = till - (avgTimeFrame * 5 + trendTimeSpan)
      for {
        bitcoinchartsTrades <- bitcoinchartsService.fetchAllTrades(bitcoinchartsSymbolMap(symbol), from, till)
        bitfinexTrades <- if(bitcoinchartsTrades.last.time < till) bitfinexExchange.fetchTradeHistory(symbol, bitcoinchartsTrades.last.time + 1, till)
          else Future.successful(Nil)
      } yield {
        val vwapData = if (bitcoinchartsTrades.head.time - from > 3600) BfxData.loadVwapData(symbol, from, bitcoinchartsTrades.head.time - 1) else Nil
        val history = new ArrayTradeHistory(vwapData ++ bitcoinchartsTrades ++ bitfinexTrades)
        val TrendProperties(startPrice, endPrice, trendFactor) = TrendFactor(history, till - trendTimeSpan, till, avgTimeFrame)
        //val startVolatilityFactor = Volatility(history, from, avgTimeFrame) / Math.abs(endPrice - startPrice)
        logger.trace(s"Time:${Instant.ofEpochSecond(till)}, start price:$startPrice, end price: $endPrice, trend factor:$trendFactor")
        Indicators(startPrice, trendFactor)
      }
    }

    protected val bitcoinchartsSymbolMap = Map[String, String]("BTCUSD" -> "bitfinexUSD")

  }

  def indicatorService: IndicatorServiceImpl

}
