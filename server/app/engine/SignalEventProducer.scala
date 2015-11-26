package engine

import monifu.reactive.{Subscriber, Observable}
import monifu.util.Random
import shared.Signal
import monifu.concurrent.FutureUtils.delayedResult
import scala.concurrent.duration.FiniteDuration

class SignalEventProducer(interval: FiniteDuration, seed: Long) extends EventProducer
  with Observable[Signal] {
  
  override def onSubscribe(subscriber: Subscriber[Signal]): Unit = {
    data.subscribe(subscriber)
  }

  val data: Observable[Signal] = Observable.create[Signal] { subscriber =>
    import subscriber.{scheduler => s}

    val random = Observable
      .fromStateAction(Random.intInRange(-20, 20))(s.currentTimeMillis() + seed)
      .flatMap { x => delayedResult(interval)(x) }

    val generator = random.scan(Signal(0, s.currentTimeMillis())) {
      case (Signal(value, _), rnd) =>
        val next = value + rnd
        Signal(next, s.currentTimeMillis())
    }

    generator
      .drop(1)
      .onSubscribe(subscriber)
  }


}
