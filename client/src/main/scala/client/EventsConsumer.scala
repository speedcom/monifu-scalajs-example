package client

import monifu.reactive.{Subscriber, Observable}
import org.scalajs.dom
import shared.{Event, Signal, OverflowEvent}
import scala.scalajs.js.Dynamic.global
import scala.concurrent.duration.FiniteDuration

final class EventsConsumer(interval: FiniteDuration, seed: Long) extends Observable[Event] {

  override def onSubscribe(subscriber: Subscriber[Event]): Unit = {
    val host     = dom.window.location.host
    val protocol = "ws:"
    val url      = s"$protocol//$host/back-pressured-stream?periodMillis=${interval.toMillis}&seed=$seed"

    val source   = BackPressuredWSClient(url)

    source
      .collect { case EventsConsumer.IsEvent(e) => e }
      .onSubscribe(subscriber)
  }

}

object EventsConsumer {

  object IsEvent {
    def unapply(message: String) = {
      val json = global.JSON.parse(message)

      json.name.asInstanceOf[String] match {
        case "signal-event" =>
          Some(Signal(
            value     = json.value.asInstanceOf[Number].doubleValue(),
            timestamp = json.timestamp.asInstanceOf[Number].longValue()
          ))
        case "overflow-event" =>
          Some(OverflowEvent(
            dropped   = json.dropped.asInstanceOf[Number].longValue(),
            timestamp = json.timestamp.asInstanceOf[Number].longValue()
          ))
        case "error" =>
          val errorType = json.`type`.asInstanceOf[String]
          val message   = json.message.asInstanceOf[String]
          throw new BackPressuredWSClient.Exception(
            s"Server-side error throw - $errorType: $message")
        case _ =>
          None
      }
    }
  }

}