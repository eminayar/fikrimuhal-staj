import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.{MessageChannel, MessageEvent, Worker, WorkerGlobalScope}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import js.Dynamic.{global => g}
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import util._

@JSExportTopLevel("MyMain")
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
  def logout(): Unit = {
    val f=Ajax.get(s"http://localhost:8080/logout?token=$token")
    f.onComplete{
      case Success(xhr) =>
        println(xhr.responseText)
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

  @JSExport
  def eraseQuote(id: Int): Unit ={
    val f = Ajax.get(s"http://localhost:8080/eraseQuote?token=$token&id=$id")
    f.onComplete{
      case Success(xhr) =>
        println(xhr.responseText)
      case Failure(e) =>
        println("not logged in!")
    }
  }

  @JSExport
  def changeQuote(id: Int, body: String): Unit ={
    val f = Ajax.get(s"http://localhost:8080/changeQuote?token=$token&id=$id&quote=$body")
    f.onComplete{
      case Success(xhr) =>
        println(xhr.responseText)
      case Failure(e) =>
        println("not logged in!")
    }
  }

  @JSExport
  def featuredQuote(): Unit ={
    val f = Ajax.get(s"http://localhost:8080/featuredQuote")
    f.onComplete{
      case Success(xhr) =>
        println(xhr.responseText)
      case Failure(e) =>
        println("not logged in!")
    }
  }

  if ( !js.isUndefined(g.navigator.serviceWorker ) ) {
    dom.window.addEventListener[dom.raw.Event]( "load" , (_) => {
      g.navigator.serviceWorker.register("worker.js").asInstanceOf[js.Promise[js.Any]].toFuture map{ e =>
        g.console.log(e)
      }
    } )
  }else{
    dom.console.log("registration failed!")
  }

  if ( !js.isUndefined(g.navigator.serviceWorker ) ) {
    g.navigator.serviceWorker.addEventListener("message" , (event: MessageEvent) => {
      dom.console.log(event.data.toString)
    })
  }

  @JSExport
  def sendMessageToSW(message: String): Unit ={
    g.navigator.serviceWorker.controller.postMessage("Client says '"+message+"'")
  }

  @JSExport
  def Download(): Unit ={
    val messageChannel = new MessageChannel()
    messageChannel.port1.onmessage = event =>{
      dom.console.log("message from SW "+event.toString)
    }
    g.navigator.serviceWorker.controller.postMessage( "hello!" , js.Array(messageChannel.port2) )
  }

  val pre = dom.document.createElement("pre")
  dom.document.body.appendChild(pre)
  val downloadButton = dom.document.createElement("BUTTON" )
  downloadButton.id="downloadButton"

}


