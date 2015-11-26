package client

import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.Observable
import shared.Signal
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import concurrent.duration._

object ScalaJsApp extends js.JSApp{

  @JSExport
  override def main(): Unit = {
    val eventsCons1 = new EventsConsumer(200 millis, 1274028492832L)
      .collect { case s: Signal => s}
    val eventsCons2 = new EventsConsumer(200 millis, 9384729038472L)
      .collect { case s: Signal => s}

    Observable.combineLatest(eventsCons1, eventsCons2)
      .onSubscribe(new SignalConsoleConsumer())
  }

}

