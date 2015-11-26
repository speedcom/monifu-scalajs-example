package client

import monifu.concurrent.Scheduler
import monifu.reactive.{Ack, Observer, Subscriber, Observable}
import org.reactivestreams.Subscription
import org.scalajs.dom.raw.WebSocket
import org.scalajs.dom._
import scala.concurrent.Future
import scala.util.Try
import concurrent.duration._

class BackPressuredWSClient private (url: String) extends Observable[String] { self =>

  override def onSubscribe(subscriber: Subscriber[String]): Unit = {
    import subscriber.scheduler

      for {
        ws      <- WSHelper.initWS(url)
        channel <- Some(WSHelper.initObsChannel(ws))
      } yield {

        val source = channel
            .timeout(5 seconds)
            .doOnCanceled(WSHelper.closeWS(ws))

        source.onSubscribe(new Observer[String] {
          override def onNext(elem: String): Future[Ack] = subscriber.onNext(elem)

          override def onError(t: Throwable): Unit = {
            WSHelper.closeWS(ws)
            scheduler.reportFailure(t)
            // retry connection in a couple of secs
            self.delaySubscription(3 seconds).onSubscribe(subscriber)
          }

          override def onComplete(): Unit = {
            WSHelper.closeWS(ws)
            // retry connection in a couple of secs
            self.delaySubscription(3 seconds).onSubscribe(subscriber)
          }
        })
      }
  }

}

object BackPressuredWSClient {
  def apply(url: String) = new BackPressuredWSClient(url)

  case class Exception(msg: String) extends RuntimeException(msg)
}

object WSHelper {

  def initObsChannel(ws: WebSocket) = Observable.create[String] {
    subscriber =>
      import subscriber.scheduler

      val downstream  = Observer.toReactiveSubscriber(subscriber)

      try {
        ws.onopen = (e: Event) => {
          downstream.onSubscribe(new Subscription {
            override def request(n: Long): Unit = ws.send(n.toString)
            override def cancel(): Unit = closeWS(ws)
          })
        }

        ws.onclose = (event: CloseEvent) => {
          downstream.onComplete()
        }

        ws.onerror = (event: ErrorEvent) => {
          downstream.onError(BackPressuredWSClient.Exception(event.message))
        }

        ws.onmessage = (event: MessageEvent) => {
          downstream.onNext(event.data.asInstanceOf[String])
        }
      } catch {
        case t: Throwable => downstream.onError(t)
      }
    }

  def initWS(url: String) = Try { new WebSocket(url) } toOption

  def closeWS(ws: WebSocket)(implicit s: Scheduler): Unit = {
    if (ws != null && ws.readyState <= 1)
      try ws.close() catch { case _: Throwable => () }
  }
}