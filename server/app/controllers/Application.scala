package controllers

import engine.BackPressuredWSActor
import play.api.libs.json.JsValue
import play.api.mvc._

object Application extends Controller with JSONFormats {

  def index = Action {
    Ok(views.html.index("app"))
  }

  def backPressuredStream(periodMillis: String, seed: Long) =
    WebSocket.acceptWithActor[String, JsValue] { _ => outActorRef =>
      BackPressuredWSActor.props(???, outActorRef)
    }


}
