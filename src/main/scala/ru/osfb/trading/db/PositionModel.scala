package ru.osfb.trading.db

import java.time.Instant

/**
  * Created by v.a.nechaev on 19.04.2016.
  */
object PositionModel {
  import ru.osfb.trading.PgDriver.api._
  class PositionTable(t: Tag) extends Table[PositionRecord](t, "POSITIONS") {

  }
}

case class PositionRecord(id: Option[Int], botId: String, openedAt: Instant, openPrice: BigDecimal, closedAt: Option[Instant], closePrice: )