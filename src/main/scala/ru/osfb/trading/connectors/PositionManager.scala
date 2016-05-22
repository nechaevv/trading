package ru.osfb.trading.connectors

import akka.actor.ActorRef
import ru.osfb.trading.db.Position
import ru.osfb.trading.strategies.PositionType.PositionType

import scala.concurrent.Future

/**
  * Created by v.a.nechaev on 20.04.2016.
  */
trait PositionManager {
  def initActorPositions(actorRef: ActorRef): Future[Seq[Position]]
  def openPositions(): Future[Seq[Position]]
  def open(actorId: String, positionType: PositionType, price: BigDecimal, quantity: BigDecimal): Future[Long]
  def close(id: Long, price: BigDecimal): Future[Unit]
}

trait PositionManagerComponent {
  def positionManager: PositionManager
}