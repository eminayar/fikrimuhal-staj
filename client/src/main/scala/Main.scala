import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.experimental.ReadableStream
import org.scalajs.dom.raw.{MessageChannel, MessageEvent, Worker, WorkerGlobalScope}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import js.Dynamic.{global => g}
import scala.concurrent.Future
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.scalajs.js.typedarray.TypedArrayBuffer
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
  def Download(): Unit ={
    val f = org.scalajs.dom.experimental.Fetch.fetch(s"http://127.0.0.1:8080/test.txt")
    f.toFuture.onComplete { resp =>
      var reader = resp.get.body.getReader()
      reader.read().toFuture.onComplete { chunk =>
        console.log("value: "+ chunk.get.value)
        console.log("done: "+ chunk.get.done)
      }
    }
//    f.asInstanceOf[Future[ReadableStream[js.Any]]].onComplete( stream =>
//      stream.map( rstream =>
//        rstream.getReader().read().toFuture map( e =>
//          dom.console.log(e.value)
//        )
//      )
//    )
//    f.onComplete {
//      case Success(xhr) =>
//        var textBox = dom.document.getElementById("pre")
//        textBox.textContent=xhr.responseText
//        var result = ""
//        val reader=xh
//        dom.console.log(reader)
//        var flag=true
//        while(flag) {
//          reader.read().toFuture map{ chunk =>
//            flag=chunk.done
//            result+=chunk.value
//          }
//        }
//        dom.console.log(result)
//      case Failure(e) =>
//        dom.console.log("Download failed!")
//    }
    f.toFuture.onComplete {
      case Success(response) =>
        val reader=response.body.getReader()

      case Failure(e) =>

    }
  }

  val pre = dom.document.createElement("pre")
  pre.id="pre"
  dom.document.body.appendChild(pre)
  val downloadButton = dom.document.createElement("BUTTON" )
  downloadButton.id="downloadButton"

}


