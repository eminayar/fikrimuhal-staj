import org.querki.jquery._
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import util._

@JSExportTopLevel("myobject")
object Main extends App{

  import dom.ext.Ajax

  private var counter=0

  def addClickedMessage(): Unit = {
    $("body").append("<p>Clicked!</p>")
  }

  @JSExport
  def ping(): Unit = {
    val f=Ajax.get("http://localhost:8080/ping")
    f.onComplete{
      case Success(xhr) =>
//        val json=js.JSON.parse(xhr.responseText)
//        val title=json.title.toString
//        val body=json.body.toString
//        println(title+" "+body)
        println(xhr.toString)
      case Failure(e) =>
        println(e.toString)
    }
  }

  @inline
  final private val prefix = "myPrefix"

  @JSExport
  def func(): Int ={
    counter += 1
    println(prefix+counter)
    counter
  }

  def setupUI(): Unit = {
    $("body").append("<p>Hello World</p>")
    $("#click-me-button").click(() => addClickedMessage())
    $("#ping-button").click(() => ping())
  }

  $(() => setupUI())

}


