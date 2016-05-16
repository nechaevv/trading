package ru.osfb.trading.tradebot

import akka.http.scaladsl.server.Directives._
import play.api.libs.json.Json
import ru.osfb.trading.connectors.PositionsServiceComponent
import ru.osfb.trading.db.Position
import ru.osfb.webapi.core.{ActorMaterializerComponent, ActorSystemComponent, ExecutionContextComponent}
import ru.osfb.webapi.http.PlayJsonMarshallers._

/**
  * Created by v.a.nechaev on 16.05.2016.
  */
trait PositionsControllerComponent { this: PositionsServiceComponent with ActorSystemComponent with ActorMaterializerComponent with ExecutionContextComponent =>
  implicit lazy val PositionWrites = Json.writes[Position]

  def positionsController = (path("positions") & get) {
    onSuccess(positionsService.openPositions()) {
      complete(_)
    }
  }

}
