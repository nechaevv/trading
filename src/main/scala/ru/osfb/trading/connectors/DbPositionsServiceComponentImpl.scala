package ru.osfb.trading.connectors
import java.time.Instant

import ru.osfb.trading.DatabaseComponent
import ru.osfb.trading.db.Position
import ru.osfb.trading.strategies.PositionType.PositionType
import ru.osfb.webapi.core.ExecutionContextComponent

import scala.concurrent.Future

/**
  * Created by sgl on 15.05.16.
  */
trait DbPositionsServiceComponentImpl extends PositionsServiceComponent {
  this: DatabaseComponent with ExecutionContextComponent =>
  import ru.osfb.trading.PgDriver.api._
  import ru.osfb.trading.db.PositionModel._

  class PositionsServiceImpl extends PositionsService {
    override def findOpenPositions(actorId: String): Future[Seq[Position]] = database run {
      positionsTable.filter(p => p.actorId === actorId && p.closedAt.isEmpty).result
    }

    override def openPositions(): Future[Seq[Position]] = database run {
      positionsTable.filter(_.closedAt.isEmpty).result
    }

    override def close(id: Long, price: BigDecimal): Future[Unit] = database run {
      positionsTable.filter(_.id === id).map(p => (p.closePrice, p.closedAt)).update(Some(price), Some(Instant.now()))
    } map (_ => ())

    override def open(actorId: String, positionType: PositionType, price: BigDecimal, quantity: BigDecimal): Future[Long] = database run {
      (positionsTable returning positionsTable.map(_.id)) += Position(None, actorId, quantity, positionType, Instant.now(), price, None, None)
    }
  }

}
