package ru.osfb.trading.tools

import java.time
import java.time.{Instant, OffsetDateTime}

import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.indicators.{Average, ExponentialMovingAverage}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import ru.osfb.webapi.utils.FutureUtils._

/**
  * Created by sgl on 02.04.16.
  */
object IndicatorTest extends App with LazyLogging {
  import ru.osfb.trading.TradingConfiguration._
  val timeFrame = time.Duration.ofDays(10)
  val to = Instant.now()
  val from = to.minus(timeFrame)
  Await.ready(for {
    Seq(avg, ema) <- Future.sequence(Seq(
      Average("bitfinexUSD", from, to),
      new ExponentialMovingAverage(timeFrame).apply("bitfinexUSD", to)
    ).map(database run _)).withErrorLog(logger)
  } yield {
    logger.info(s"AVG: $avg, EMA: $ema")
  }, 1.minute)

}
