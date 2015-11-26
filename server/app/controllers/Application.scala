package controllers

import engine.{SignalEventProducer, BackPressuredWSActor}
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.Play.current
import concurrent.duration._
import monifu.concurrent.Implicits.globalScheduler

object Application extends Controller with JSONFormats {

  def index = Action {
    Ok(views.html.index())
  }

  def backPressuredStream(periodMillis: Int, seed: Long) =
    WebSocket.acceptWithActor[String, JsValue] { _ => outActorRef =>
      BackPressuredWSActor.props(new SignalEventProducer(periodMillis.millis, seed), outActorRef)
    }

}
