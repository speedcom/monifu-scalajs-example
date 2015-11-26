package client

import monifu.reactive.Ack.Continue
import monifu.reactive.{Ack, Observer}
import shared.Signal

import scala.concurrent.Future

final class SignalConsoleConsumer extends Observer[(Signal, Signal)] {

  override def onNext(elem: (Signal, Signal)): Future[Ack] = {
    println("elem: " + elem)
    Continue
  }

  override def onError(ex: Throwable): Unit = {
    System.err.println(s"ERROR: $ex")
  }

  override def onComplete(): Unit = ()
}
