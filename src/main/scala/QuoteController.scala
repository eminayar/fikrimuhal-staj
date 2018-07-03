import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.Cluster


final case class Quote(id: Int, body: String)
final case class QuoteActionPerformed( description: String )
final case class CreateQuote(body: String)
final case object GetQuotes
final case class EraseQuote(id: Int)
final case class ChangeQuote(id: Int, body: String)
final case object FeaturedQuote

class QuoteController extends Actor {

  val cluster=Cluster(context.system)

  var quoteAct: ActorSelection = _

  override def preStart(): Unit = cluster.subscribe(self , initialStateMode = InitialStateAsEvents , classOf[MemberEvent] , classOf[UnreachableMember] )

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case MemberUp(m) =>
      println("$$$$" + m)
      if (m.hasRole("quoteActor")) {
        println("$$$$found a quote actor")
        quoteAct = context.actorSelection(RootActorPath(m.address) / "user" / "quoteActor")
      }
    case CreateQuote( body ) =>
      quoteAct forward CreateQuote(body)
    case GetQuotes =>
      quoteAct forward GetQuotes
    case EraseQuote( id ) =>
      quoteAct forward EraseQuote(id)
    case ChangeQuote(id,body) =>
      quoteAct forward ChangeQuote(id,body)
    case FeaturedQuote =>
      quoteAct forward FeaturedQuote
  }

}