package controllers

import play.api.mvc._
import shared.SharedMessages

object Application extends Controller with JSONFormats {

  def index = Action {
    Ok(views.html.index("app"))
  }



}
