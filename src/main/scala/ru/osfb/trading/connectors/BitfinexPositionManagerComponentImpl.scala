package ru.osfb.trading.connectors
import java.time.Instant
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props, ReceiveTimeout}
import com.typesafe.scalalogging.LazyLogging
import ru.osfb.trading.DatabaseComponent
import ru.osfb.trading.connectors.BitfinexProtocol.{Balance, OrderSide, OrderStatus, OrderType}
import ru.osfb.trading.db.{Position, PositionStatus}
import ru.osfb.trading.strategies.PositionType
import ru.osfb.trading.strategies.PositionType.PositionType
import ru.osfb.webapi.core.{ActorSystemComponent, ConfigurationComponent, ExecutionContextComponent}
import ru.osfb.webapi.utils.FutureUtils._

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by sgl on 15.05.16.
  */
trait BitfinexPositionManagerComponentImpl extends PositionManagerComponent {
  this: DatabaseComponent
    with ExecutionContextComponent
    with ActorSystemComponent
    with ConfigurationComponent
    with BitfinexExchangeComponent =>
  import ru.osfb.trading.PgDriver.api._
  import ru.osfb.trading.db.PositionModel._

  override def positionManager: PositionManagerImpl

  class PositionManagerImpl(base: String, instrument: String) extends PositionManager with LazyLogging {
    override def initActorPositions(actorRef: ActorRef): Future[Seq[Position]] = database run {
      positionsTable.filter(p => p.actorId === actorRef.path.name && p.positionStatus =!= PositionStatus.Closed).result
    } flatMap { positions =>
      Future.sequence(positions map checkPendingState)
    } map (_.filter(_.positionStatus != PositionStatus.Cancelled))

    override def close(id: Long, price: BigDecimal, orderTimeLimit: FiniteDuration, listener: ActorRef): Future[Unit] = for {
      position <- database run positionsTable.filter(_.id === id).result.head
      balances <- getBalances
      (side, amount) = position.positionType match {
        case PositionType.Long => (OrderSide.Sell, (balances.instrument / 2) - positionOpenThreshold)
        case PositionType.Short => (OrderSide.Buy, (balances.base / (2 * price)) - positionOpenThreshold)
      }
      orderStatus <- bitfinexExchange.newOrder(instrument, side, OrderType.ExchangeLimit, amount, price)
      _ <- database run positionsTable.filter(_.id === id).map(p => (p.closeOrderId, p.closePrice, p.positionStatus)).update(Some(orderStatus.id), Some(price), PositionStatus.PendingClose)
    } yield {
      actorSystem.scheduler.scheduleOnce(orderTimeLimit, new Runnable {
        override def run(): Unit = {
          (for {
            position <- database run positionsTable.filter(_.id === id).result.head
            pendingCheckResult <- checkPendingState(position, listener)
            _ <- if (pendingCheckResult.isEmpty) Future.successful(()) else (for {
              cancelStatus <- bitfinexExchange.cancelOrder(orderStatus.id) if cancelStatus.isCancelled
              forceOrderStatus <- bitfinexExchange.newOrder(instrument, side, OrderType.ExchangeMarket, amount, price)
              forcedPosition <- database run {
                val q = positionsTable.filter(_.id === id)
                q.map(_.closeOrderId).update(Some(forceOrderStatus.id)) andThen q.result.head
              }
            } yield {
              actorSystem.scheduler.scheduleOnce(forceOrderCheckInterval, new Runnable {
                override def run(): Unit = checkPendingState(forcedPosition, listener)
              })
            }) withErrorLog logger
          } yield ()) withErrorLog logger
        }
      })
      ()
    }

    override def open(actorId: String, positionType: PositionType, price: BigDecimal, orderTimeLimit: FiniteDuration, listener: ActorRef): Future[Long] = for {
      balances <- getBalances
      (side, amount) = positionType match {
        case PositionType.Long => (OrderSide.Buy, (balances.base / price) - positionOpenThreshold)
        case PositionType.Short => (OrderSide.Sell, balances.instrument - positionOpenThreshold)
      }
      orderStatus <- bitfinexExchange.newOrder(instrument, side, OrderType.ExchangeLimit, amount, price)
      positionId <- database run {
        (positionsTable returning positionsTable.map(_.id)) += Position(None, actorId, amount, positionType,
          PositionStatus.PendingOpen, Some(orderStatus.id), Some(Instant.now().plusSeconds(orderTimeLimit.toSeconds)), Some(price),
          None, None)
      }
    } yield {
      actorSystem.scheduler.scheduleOnce(orderTimeLimit, new Runnable {
        override def run(): Unit = {
          (for {
            position <- database run positionsTable.filter(_.id === positionId).result.head
            pendingCheckResult <- checkPendingState(position, listener)
            _ <- if (pendingCheckResult.isEmpty) Future.successful(()) else (for {
              cancelStatus <- bitfinexExchange.cancelOrder(orderStatus.id) if cancelStatus.isCancelled
              forceOrderStatus <- bitfinexExchange.newOrder(instrument, side, OrderType.ExchangeMarket, amount, price)
              forcedPosition <- database run {
                val q = positionsTable.filter(_.id === positionId)
                q.map(_.pendingOrderId).update(Some(forceOrderStatus.id)) andThen q.result.head
              }
            } yield {
              actorSystem.scheduler.scheduleOnce(forceOrderCheckInterval, new Runnable {
                override def run(): Unit = checkPendingState(forcedPosition, listener)
              })
            }) withErrorLog logger
          } yield ()) withErrorLog logger
        }
      })
      positionId
    }

    def checkPendingState(position: Position, listener: ActorRef): Future[Option[Position]] = (position.positionStatus match {
      case PositionStatus.PendingOpen | PositionStatus.PendingClose => position.pendingOrderId
      case _ => None
    }) match {
      case None => Future.successful(None)
      case Some(pendingOrderId) => bitfinexExchange.orderStatus(pendingOrderId) flatMap { orderStatus =>
        val positionQ = positionsTable.filter(_.id === position.id)
        if (orderStatus.isActive) Future.successful(None) else {
          (if (orderStatus.isCancelled) {
            val newStatus = position.positionStatus match {
              case PositionStatus.PendingOpen => PositionStatus.Cancelled
              case PositionStatus.PendingClose => PositionStatus.Opened
            }
            database run {
              positionQ.map(_.positionStatus).update(newStatus) andThen positionQ.result.head
            } map (listener ! _)
          } else {
            val (q, newStatus) = position.positionStatus match {
              case PositionStatus.PendingOpen => (positionQ.map(p => (p.openAt, p.openPrice, p.quantity, p.positionStatus, p.pendingOrderId)), PositionStatus.Opened)
              case PositionStatus.PendingClose => (positionQ.map(p => (p.closeAt, p.closePrice, p.quantity, p.positionStatus, p.pendingOrderId)), PositionStatus.Closed)
            }
            database run {
              q.update(Some(Instant.ofEpochSecond(orderStatus.timestamp)), Some(orderStatus.averagePrice), orderStatus.amount, newStatus, None) andThen q.result.head
            } map (listener ! _)
          }) withErrorLog logger
        }.flatMap(_ => database run positionQ.result.headOption)
      }
    }
    protected def getBalances = bitfinexExchange.balances.map(_.filter(_.`type` == "exchange").foldLeft(Balances(0, 0))((balances, walletBalance) => if (walletBalance.currency == base)
      balances.copy(base = walletBalance.amount) else if (walletBalance.currency == instrument) balances.copy(instrument = walletBalance.amount) else balances))
  }

  case class Balances(base: BigDecimal, instrument: BigDecimal)

  lazy val orderStatusPollInterval = configuration.getDuration("bitfinex.order-status-poll-interval", TimeUnit.SECONDS).seconds
  lazy val forceOrderCheckInterval = configuration.getDuration("bitfinex.force-order-check-interval", TimeUnit.SECONDS).seconds
  lazy val positionOpenThreshold = BigDecimal(configuration.getString("bitfinex.position-open-threshold"))

}
