import org.querki.jquery._
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js.timers._
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import util._

@JSExportTopLevel("myobject")
object Main extends App{

  import dom.ext.Ajax

  private var token=""

  @JSExport
  def ping(): Unit = {
    val f = Ajax.get(s"http://localhost:8080/ping?token=$token")
    f.onComplete {
      case Success(xhr) =>
        val now = System.currentTimeMillis() - xhr.responseText.toLong
        println(now.toString)
      case Failure(e) =>
        println("not logged in!")
    }
  }

  @JSExport
  def login(uname: String, passwd: String): Unit = {
    val f=Ajax.get(s"http://localhost:8080/login?username=$uname&password=$passwd")
    f.onComplete{
      case Success(xhr) =>
        token = xhr.responseText
        println("Token:"+token)
      case Failure(e) =>
        println("Error:"+e.toString)
    }
  }

  @JSExport
  def createQuote(body: String): Unit ={
    val f = Ajax.get(s"http://localhost:8080/createQuote?token=$token&quote=$body")
    f.onComplete{
      case Success(xhr) =>
        println(xhr.responseText)
      case Failure(e) =>
        println("not logged in!")
    }
  }


}


