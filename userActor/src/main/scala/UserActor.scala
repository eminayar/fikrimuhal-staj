import akka.actor._
import java.time.Instant

import akka.cluster.ClusterEvent._
import akka.cluster.Cluster
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
  }

  def props(): Props = Props(new UserActor())
}

class UserActor extends Actor with ActorLogging {

  private var state = UserState()
  private var activeTokens = Set.empty[String]
  private final val secret="topsecret"
  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self , initialStateMode = InitialStateAsEvents , classOf[MemberEvent] , classOf[UnreachableMember] )
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case MemberUp(m) =>
      println("$$$$"+m)
      if(m.hasRole("quoteActor") ){
        println("$$$$found a quote actor")
        context.actorSelection(RootActorPath(m.address) / "user" / "quoteActor") ! "INTER CLUSTER MESSAGE!"
      }
    case CreateUser(user) =>
      println("create")
      state=state.created(user)
      sender ! "Created"
    case GetUsers =>
      println("get users query from:"+sender())
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
