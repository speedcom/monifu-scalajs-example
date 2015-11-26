package controllers

import play.api.libs.json._
import shared.Signal

trait JSONFormats {
  private val defaultPointFormat = Json.format[Signal]

  implicit val pointFormat = new Format[Signal] {
    def reads(json: JsValue): JsResult[Signal] =
      (json \ "name").validate[String].flatMap {
        case "signal-event" =>
          defaultPointFormat.reads(json)
        case _ =>
          JsError(JsPath \ "name", s"Event is not `point`")
      }

    def writes(o: Signal): JsValue =
      Json.obj("name" -> o.name) ++
        defaultPointFormat.writes(o).as[JsObject]
  }
}
