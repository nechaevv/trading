package ru.osfb.trading.tradebot

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import play.api.libs.json.Json
import ru.osfb.webapi.core.ExecutionContextComponent
import ru.osfb.webapi.http.PlayJsonMarshallers._
/**
  * Created by sgl on 09.04.16.
  */
trait IndicatorControllerComponent {
    this: ExecutionContextComponent =>

  //implicit val indWrites = Json.writes[Indicators]

  def indicatorController = path("indicators" / Segment) { symbol =>
    //onSuccess(indicatorService.computeIndicators(symbol)) {
      complete(StatusCodes.NotImplemented)
    //}
  }

}
