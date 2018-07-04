import SingletonExample._
import akka.actor._
import akka.cluster.Cluster
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn

object WebServer extends App with Routes{

  val config = ConfigFactory.parseString("""
       akka.actor.provider = cluster
    """).withFallback(ConfigFactory.parseString("akka.cluster.roles = [server]"))
    .withFallback(ConfigFactory.load())
  implicit val system = ActorSystem("clusterSystem" , config)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val cluster = Cluster(system)
  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  val userActor: ActorRef = system.actorOf(Props[UserController])
  val quoteActor: ActorRef = system.actorOf(Props[QuoteController])
  system.actorOf(
    ClusterSingletonManager.props(
      singletonProps = Props(classOf[SingletonExample]),
      terminationMessage = End,
      settings = ClusterSingletonManagerSettings(system)),
    name = "singleton")


  lazy val routes: Route = myRoutes

  val bindingFuture = Http().bindAndHandle(routes, "0.0.0.0" , 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  Await.result(system.whenTerminated, Duration.Inf)

}
