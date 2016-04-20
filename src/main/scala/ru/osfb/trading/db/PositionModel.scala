package ru.osfb.trading.db

import java.time.Instant

import ru.osfb.trading.strategies.PositionType.PositionType

/**
  * Created by v.a.nechaev on 19.04.2016.
  */
object PositionModel {
  import ru.osfb.trading.PgDriver.api._
  class PositionTable(t: Tag) extends Table[PositionRecord](t, "POSITIONS") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def actorId = column[String]("ACTOR_ID")
    def quantity = column[BigDecimal]("QUANTITY")
    def positionType = column[PositionType]("POSITION_TYPE")
    def openedAt = column[Instant]("OPENED_AT")
    def openPrice = column[BigDecimal]("OPEN_PRICE")
    def closedAt = column[Option[Instant]]("CLOSED_AT")
    def closePrice = column[Option[BigDecimal]]("CLOSE_PRICE")
    def * = (id.?, actorId, quantity, positionType, openedAt, openPrice, closedAt, closePrice) <> (PositionRecord.tupled, PositionRecord.unapply)
    def actorIdx = index("POSITIONS_ACTOR_IDX", actorId, unique = false)
  }
  val positionsTable = TableQuery[PositionTable]
}

case class PositionRecord
(
  id: Option[Int],
  actorId: String,
  quantity: BigDecimal,
  openedAt: Instant,
  openPrice: BigDecimal,
  closedAt: Option[Instant],
  closePrice: Option[BigDecimal]
)