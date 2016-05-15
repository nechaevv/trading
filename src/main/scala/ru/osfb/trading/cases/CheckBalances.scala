package ru.osfb.trading.cases

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by sgl on 14.05.16.
  */
object CheckBalances extends App {
  import ru.osfb.trading.tradebot.ComponentWiring._
  try {
    val balances = Await.result(bitfinexExchange.balances, 1.minute)
    for (balance <- balances) println(balance)
  } finally {
    actorSystem.terminate()
  }

}
