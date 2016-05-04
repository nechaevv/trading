package ru.osfb.trading.calculations

/**
  * Created by sgl on 08.04.16.
  */
case class Trade(time: Long, amount: Double, quantity: Double) {
  def price = amount/quantity
}