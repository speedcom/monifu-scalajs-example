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

class BackPressuredWSActor[T <: Event : json.Writes](dataProducer: Observable[T], out: ActorRef)
  extends Actor
  with LazyLogging {

  override def receive = {
    case BackPressuredWSActor.Request(nr) => subscription.request(nr)
  }

  private[this] val subscription = SingleAssignmentSubscription()
}

object BackPressuredWSActor {

  def props[T <: Event : json.Writes](dataProducer: Observable[T], out: ActorRef) =
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

}