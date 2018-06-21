import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, RootActorPath}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.io.StdIn



object WebServer extends App with Routes{

  implicit val system = ActorSystem("ClusterSystem")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val userActor: ActorRef = UserActor.main(Seq("2561").toArray)

  val quoteActor: ActorRef = QuoteActor.main(Seq("2562").toArray)

  lazy val routes: Route = myRoutes

  val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  userActor ! PoisonPill
  quoteActor ! PoisonPill
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}
