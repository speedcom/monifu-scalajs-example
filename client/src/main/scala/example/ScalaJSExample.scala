package example

import scala.scalajs.js
import org.scalajs.dom
import shared.SharedMessages

object ScalaJSExample extends js.JSApp {
  def main() = {
    dom.document.getElementById("scalajsShoutOut").textContent = SharedMessages.itWorks
  }
}