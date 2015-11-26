package engine

import akka.actor.{Props, ActorRef, Actor}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json
import play.api.libs.json.{JsObject, JsValue, Json}
import shared.Event
import scala.concurrent.duration._
import monifu.concurrent.Scheduler
import monifu.reactive.Observable
import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.streams.SingleAssignmentSubscription
import org.reactivestreams.{Subscriber, Subscription}
import BackPressuredWSActor._

class BackPressuredWSActor[T <: Event : json.Writes](signalProducer: Observable[T], out: ActorRef)
                                                    (implicit s: Scheduler)
  extends Actor
  with LazyLogging {

  override def receive = {
    case BackPressuredWSActor.Request(nr) => subscription.request(nr)
  }

  private[this] val subscription = SingleAssignmentSubscription()

  override def preStart() {
    super.preStart()

    val dataSource = {
      val initSignal        = Observable.unit(initMsg(now()))
      val continuousSignals = signalProducer.map(Json.toJson(_))
      val obs               = initSignal ++ continuousSignals
      val timeout           = obs.debounceRepeated(5 seconds).map(_ => keepAliveMessage(now()))

      Observable.merge(obs, timeout)
        .whileBusyBuffer(DropOld(100), nr => onOverflow(nr, now()))
    }

    dataSource.toReactive.subscribe(new Subscriber[JsValue] {
      override def onSubscribe(s: Subscription): Unit = {
        subscription := s
      }

      override def onError(throwable: Throwable): Unit = ???

      override def onComplete(): Unit = ???

      override def onNext(t: JsValue): Unit = ???
    })
  }
}

object BackPressuredWSActor {

  def now() = System.currentTimeMillis()

  def props[T <: Event : json.Writes](dataProducer: Observable[T], out: ActorRef)(implicit s: Scheduler): Props =
    Props(new BackPressuredWSActor(dataProducer, out))

  object Request {
    def unapply(value: Any): Option[Long] = value match {
      case str: String =>
        str.trim match {
          case IsInteger(integer) =>
            try Some(integer.toLong).filter(_ > 0) catch {
              case _ : NumberFormatException => None
            }
          case _ => None
        }
      case number: Int =>
        Some(number.toLong)
      case number: Long =>
        Some(number)
      case _ =>
        None
    }

    val IsInteger = """^([-+]?\d+)$""".r
  }

  def onOverflow(dropped: Long, now: Long) = {
    Json.obj(
      "event" -> "overflow",
      "dropped" -> dropped,
      "timestamp" -> now
    )
  }

  def initMsg(now: Long) = {
    Json.obj("event" -> "init", "timestamp" -> now)
  }

  def keepAliveMessage(now: Long) = {
    Json.obj("event" -> "keep-alive", "timestamp" -> now)
  }

}