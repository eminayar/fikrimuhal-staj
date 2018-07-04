import akka.actor._
import java.time.Instant

import SingletonExample._
import akka.cluster.ClusterEvent._
import akka.cluster.Cluster
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.persistence._
import com.typesafe.config.ConfigFactory
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

final case class UserState(users: List[User] = Nil ){
  def created( user:User ): UserState = copy( user :: users )
  override def toString: String = users.reverse.toString
}

object UserActor{

  def main(args: Array[String] ): Unit = {
    println("hello v:1.0")
    val config = ConfigFactory.parseString(
      """
       akka.actor.provider = cluster
    """).withFallback(ConfigFactory.parseString("akka.cluster.roles = [userActor]"))
      .withFallback(ConfigFactory.load())
    val system = ActorSystem("clusterSystem", config)
    implicit val cluster = Cluster(system)
    AkkaManagement(system).start()
    ClusterBootstrap(system).start()
    system.actorOf(UserActor.props, "userActor")
    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(classOf[SingletonExample]),
        terminationMessage = End,
        settings = ClusterSingletonManagerSettings(system)),
      name = "singleton")
  }

  def props(): Props = Props(new UserActor())
}

class UserActor extends Actor with ActorLogging {

  private var state = UserState()
  private var activeTokens = Set.empty[String]
  private final val secret="topsecret"
  var proxy: ActorRef = _
  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self , initialStateMode = InitialStateAsEvents , classOf[MemberEvent] , classOf[UnreachableMember] )
    proxy = context.actorOf(ClusterSingletonProxy.props(
      settings = ClusterSingletonProxySettings(context.system),
      singletonManagerPath = "/user/singleton"
    ),
      name="proxy")
    log.info("user actor started")
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case CreateUser(user) =>
      println("create")
      state=state.created(user)
      sender ! "Created"
    case GetUsers =>
      log.info("sending message to singleton")
      proxy ! "hello from user actor"
      sender ! state.users
    case SaveSnapshotSuccess(metadata) =>
      println("snapshot success")
    case SaveSnapshotFailure(metadata,reason) =>
      println("snapshot failed:"+reason.toString)
    case Login(user) =>
      if( state.users.contains(user) ){
        val claim = JwtClaim(
          expiration = Some(Instant.now.plusSeconds(2700).getEpochSecond),
          issuedAt = Some(Instant.now.getEpochSecond),
          content = user.username
        )
        val token=JwtCirce.encode( claim , secret , JwtAlgorithm.HS256 )
        activeTokens+=token
        sender ! token
      }else{
        sender ! "invalid credentials"
      }
    case Logout(token) =>
      activeTokens-=token
      sender ! "OK"
    case isValidToken(token) =>
      val decoded = JwtCirce.decode( token, secret , Seq(JwtAlgorithm.HS256) )
      if( decoded.isFailure ){
        sender ! false
      }else{
        if( decoded.get.expiration.get < Instant.now.getEpochSecond ){
          activeTokens -= token
        }
        sender ! activeTokens.contains(token)
      }
  }

}
