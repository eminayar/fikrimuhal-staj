import SingletonExample._
import akka.actor._
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.http.scaladsl.server.Route
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.util.Success

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
  val store: ActorRef = system.actorOf(Props[SharedLeveldbStore],"store")
  val f = store ? Identify(None)
  f.onComplete{
    case Success(ActorIdentity(_,Some(ref) )) => SharedLeveldbJournal.setStore(ref , system)
    case Success(smth) => println(smth.toString)
    case _ => println("$$$$$$$$$$$$$$$$$failure!")
  }
  system.actorOf(
    ClusterSingletonManager.props(
      singletonProps = Props(classOf[SingletonExample]),
      terminationMessage = End,
      settings = ClusterSingletonManagerSettings(system)),
    name = "singleton")
  ClusterSharding(system).start(
    typeName = "testShardRegion",
    entityProps = Props[testActor],
    settings = ClusterShardingSettings(system),
    extractEntityId = testActor.idExtractor,
    extractShardId = testActor.shardResolver
  )


  lazy val routes: Route = myRoutes

  val bindingFuture = Http().bindAndHandle(routes, "0.0.0.0" , 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  Await.result(system.whenTerminated, Duration.Inf)

}
