package ru.osfb.trading.connectors

import ru.osfb.trading.db.PositionRecord

import scala.concurrent.Future

/**
  * Created by v.a.nechaev on 20.04.2016.
  */
trait PositionsService {
  def findOpenPositions(actorId: String): Future[Seq[PositionRecord]]
  def open(actorId: String, price: BigDecimal, quantity: BigDecimal): Future[Long]
  def close(id: Long, actorId: String, price: BigDecimal): Future[Unit]
}

trait PositionsServiceComponent {
  def positionsService: PositionsService
}