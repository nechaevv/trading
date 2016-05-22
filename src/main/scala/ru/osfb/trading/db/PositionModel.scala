package ru.osfb.trading.db

import java.time.Instant

import ru.osfb.trading.db.PositionStatus.PositionStatus
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
  implicit val positionStatusColumnType = MappedColumnType.base[PositionStatus, String](
    {pt => pt.toString}, {s => PositionStatus.withName(s)}
  )
  class PositionTable(t: Tag) extends Table[Position](t, "POSITIONS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def actorId = column[String]("ACTOR_ID")
    def quantity = column[BigDecimal]("QUANTITY")
    def positionType = column[PositionType]("POSITION_TYPE")
    def positionStatus = column[PositionStatus]("POSITION_STATUS")
    def openOrderId = column[Option[Long]]("OPEN_ORDER_ID")
    def openAt = column[Option[Instant]]("OPEN_AT")
    def openPrice = column[Option[BigDecimal]]("OPEN_PRICE")
    def closeOrderId = column[Option[Long]]("CLOSE_ORDER_ID")
    def closeAt = column[Option[Instant]]("CLOSE_AT")
    def closePrice = column[Option[BigDecimal]]("CLOSE_PRICE")
    def * = (id.?, actorId, quantity, positionType, positionStatus, openOrderId, openAt, openPrice, closeOrderId,
      closeAt, closePrice) <> (Position.tupled, Position.unapply)
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
  positionStatus: PositionStatus,
  openOrderId: Option[Long],
  openAt: Option[Instant],
  openPrice: Option[BigDecimal],
  closeOrderId: Option[Long],
  closeAt: Option[Instant],
  closePrice: Option[BigDecimal]
)

object PositionStatus extends Enumeration {
  type PositionStatus = Value
  val PendingOpen = Value("PO")
  val Opened = Value("O")
  val PendingClose = Value("PC")
  val Closed = Value("C")
  val Cancelled = Value("R")
}