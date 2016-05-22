package ru.osfb.trading.connectors
import java.time.Instant
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props, ReceiveTimeout}
import ru.osfb.trading.DatabaseComponent
import ru.osfb.trading.connectors.BitfinexProtocol.Balance
import ru.osfb.trading.db.{Position, PositionStatus}
import ru.osfb.trading.strategies.PositionType.PositionType
import ru.osfb.webapi.core.{ActorSystemComponent, ConfigurationComponent, ExecutionContextComponent}

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

  class PositionManagerImpl(baseCurrency: String, positionCurrency: String) extends PositionManager {
    override def initActorPositions(actorRef: ActorRef): Future[Seq[Position]] = database run {
      positionsTable.filter(p => p.actorId === actorRef.path.name && p.positionStatus =!= PositionStatus.Closed).result
    } flatMap { positions =>
      Future.sequence(positions map checkPendingState)
    } map (_.filter(_.positionStatus != PositionStatus.Cancelled))

    override def close(id: Long, price: BigDecimal): Future[Unit] = database run {
      positionsTable.filter(_.id === id).map(p => (p.closePrice, p.closeAt)).update(Some(price), Some(Instant.now()))
    } map (_ => ())

    override def open(actorId: String, positionType: PositionType, price: BigDecimal, quantity: BigDecimal): Future[Long] = {
      bitfinexExchange.balances.collect {
        case Balance("exchange", base)
      }


      database run {
        (positionsTable returning positionsTable.map(_.id)) += Position(None, actorId, quantity, positionType, Instant.now(), price, None, None)
      }
    }

    def checkPendingState(position: Position): Future[Option[Position]] = (position.positionStatus match {
      case PositionStatus.PendingOpen => position.openOrderId
      case PositionStatus.PendingClose => position.closeOrderId
      case _ => None
    }) match {
      case None => Future.successful(None)
      case Some(pendingOrderId) => bitfinexExchange.orderStatus(pendingOrderId) flatMap { orderStatus =>
        val positionQ = positionsTable.filter(_.id === position.id)
        if (orderStatus.isActive) Future.successful(None) else {
          if (orderStatus.isCancelled) {
            val newStatus = position.positionStatus match {
              case PositionStatus.PendingOpen => PositionStatus.Cancelled
              case PositionStatus.PendingClose => PositionStatus.Opened
            }
            database run positionQ.map(_.positionStatus).update(newStatus)
          } else {
            val (q, newStatus) = position.positionStatus match {
              case PositionStatus.PendingOpen => (positionQ.map(p => (p.openAt, p.openPrice, p.positionStatus)), PositionStatus.Opened)
              case PositionStatus.PendingClose => (positionQ.map(p => (p.closeAt, p.closePrice, p.positionStatus)), PositionStatus.Closed)
            }
            database run {
              q.update(Some(Instant.ofEpochSecond(orderStatus.timestamp)), Some(orderStatus.averagePrice), newStatus)
            }
          }
        }.flatMap(_ => database run positionQ.result.headOption)
      }
    }
  }

  lazy val orderStatusPollInterval = configuration.getDuration("bitfinex.order-status-poll-interval", TimeUnit.SECONDS).seconds
  lazy val positionOpenThreshold = BigDecimal(configuration.getString("bitfinex.position-open-threshold"))

}
