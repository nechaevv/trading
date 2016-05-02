package ru.osfb.trading.db

import java.time.Instant

import ru.osfb.trading.strategies.PositionType
import ru.osfb.trading.strategies.PositionType.PositionType

/**
  * Created by v.a.nechaev on 19.04.2016.
  */
object PositionModel {
  import ru.osfb.trading.PgDriver.api._
  implicit val positionTypeColumnType = MappedColumnType.base[PositionType, String](
    {pt => pt.toString}, {s => PositionType.withName(s)}
  )
  class PositionTable(t: Tag) extends Table[Position](t, "POSITIONS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def actorId = column[String]("ACTOR_ID")
    def quantity = column[BigDecimal]("QUANTITY")
    def positionType = column[PositionType]("POSITION_TYPE")
    def openedAt = column[Instant]("OPENED_AT")
    def openPrice = column[BigDecimal]("OPEN_PRICE")
    def closedAt = column[Option[Instant]]("CLOSED_AT")
    def closePrice = column[Option[BigDecimal]]("CLOSE_PRICE")
    def * = (id.?, actorId, quantity, positionType, openedAt, openPrice, closedAt, closePrice) <> (Position.tupled, Position.unapply)
    def actorIdx = index("POSITIONS_ACTOR_IDX", actorId, unique = false)
  }
  val positionsTable = TableQuery[PositionTable]
}

case class Position
(
  id: Option[Long],
  actorId: String,
  quantity: BigDecimal,
  positionType: PositionType,
  openedAt: Instant,
  openPrice: BigDecimal,
  closedAt: Option[Instant],
  closePrice: Option[BigDecimal]
)