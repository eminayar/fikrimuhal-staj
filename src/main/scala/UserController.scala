import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.Cluster

class UserController extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  var userAct: ActorSelection = _

  override def preStart(): Unit = {
    cluster.subscribe(self , initialStateMode = InitialStateAsEvents , classOf[MemberEvent] , classOf[UnreachableMember] )
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case MemberUp(m) =>
      println("$$$$" + m)
      if (m.hasRole("userActor")) {
        println("$$$$found a user actor")
        userAct = context.actorSelection(RootActorPath(m.address) / "user" / "userActor")
        println(userAct)
      }
    case GetUsers =>
      userAct forward GetUsers
    case CreateUser(user) =>
      userAct forward CreateUser(user)
    case Login(user) =>
      userAct forward Login(user)
    case Logout(token) =>
      userAct forward Logout(token)
    case isValidToken(token) =>
      userAct forward isValidToken(token)
  }

}
