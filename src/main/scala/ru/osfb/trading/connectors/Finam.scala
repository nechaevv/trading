package ru.osfb.trading.connectors

import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneId}
import java.time.format.DateTimeFormatter

import ru.osfb.trading.calculations.Trade

import scala.io.Source

/**
  * Created by v.a.nechaev on 11.04.2016.
  */
object Finam {
  val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")
  val timeFormat = DateTimeFormatter.ofPattern("HHmmss")
  def loadCsvTrades(fileName: String): Seq[Trade] = Source.fromFile(fileName).getLines().drop(1).map(_.split(",") match {
    case Array(_,_,dateStr,timeStr,priceStr,quantityStr) => {
      val time = LocalDate.from(dateFormat.parse(dateStr)).atTime(LocalTime.from(timeFormat.parse(timeStr))).atZone(ZoneId.systemDefault()).toInstant.getEpochSecond
      Trade(time, priceStr.toDouble * quantityStr.toDouble, quantityStr.toDouble)
    }
  }).toSeq
}
