package ru.osfb.trading.feeds

import akka.actor.ActorRef
import akka.event.{EventBus, LookupClassification}

/**
  * Created by v.a.nechaev on 19.04.2016.
  */
object HistoryUpdateEventBus extends EventBus with LookupClassification {
  type Classifier = HistoryUpdateEvent
  type Event = HistoryUpdateEvent
  type Subscriber = ActorRef

  override protected def mapSize(): Int = 16

  override protected def classify(event: HistoryUpdateEvent): HistoryUpdateEvent = event

  override protected def publish(event: HistoryUpdateEvent, subscriber: ActorRef): Unit = {
    subscriber ! event
  }

  override protected def compareSubscribers(a: ActorRef, b: ActorRef): Int = a compareTo b
}

case class HistoryUpdateEvent(exchange: String, symbol: String)