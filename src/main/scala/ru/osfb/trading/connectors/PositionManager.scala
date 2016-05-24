package ru.osfb.trading.connectors

import akka.actor.ActorRef
import ru.osfb.trading.db.Position
import ru.osfb.trading.strategies.PositionType.PositionType

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Created by v.a.nechaev on 20.04.2016.
  */
trait PositionManager {
  def initActorPositions(actorRef: ActorRef): Future[Seq[Position]]
  def openPositions(): Future[Seq[Position]]
  def open(actorId: String, positionType: PositionType, price: BigDecimal, orderTimeLimit: FiniteDuration): Future[Long]
  def close(id: Long, price: BigDecimal, orderTimeLimit: FiniteDuration): Future[Unit]
}

trait PositionManagerComponent {
  def positionManager: PositionManager
}