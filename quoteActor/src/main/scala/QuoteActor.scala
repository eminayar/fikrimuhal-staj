import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.ClusterEvent.{ClusterDomainEvent, InitialStateAsEvents, MemberEvent, UnreachableMember}
import akka.cluster.{Cluster, ClusterEvent}
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.persistence.PersistentActor
import akka.routing.RoundRobinPool
import com.typesafe.config.ConfigFactory

object QuoteActor {

  def main(args: Array[String]): Unit = {
    println("hello")
    val config = ConfigFactory.parseString("""
       akka.actor.provider = cluster
    """).withFallback(ConfigFactory.parseString("akka.cluster.roles = [quoteActor]"))
      .withFallback(ConfigFactory.load())
    val system = ActorSystem("clusterSystem" , config)
    implicit val cluster = Cluster(system)
    AkkaManagement(system).start()
    ClusterBootstrap(system).start()
    system.actorOf(QuoteActor.props, "quoteActor" )
  }

  def props: Props = Props(new QuoteActor)
}

final case class QuoteState( quotes: List[Quote] = Nil , idCounter: Int = 0 ){
  def created( body: String ): QuoteState = copy( Quote(idCounter+1, body) :: quotes , idCounter+1 )
  def erased( id: Int ): QuoteState ={
    val newList = quotes.filter( q => q.id != id )
    QuoteState( newList , idCounter )
  }
  def changed( id: Int , body: String ): QuoteState = {
    val newList = quotes.map( q =>
      if( q.id == id ) Quote( id , body )
      else q
    )
    QuoteState( newList , idCounter )
  }
  override def toString: String = quotes.reverse.toString
}

class QuoteActor extends Actor {

//  override def persistenceId: String = "quote-actor-id-1"

  val cluster=Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self , initialStateMode = InitialStateAsEvents , classOf[MemberEvent] , classOf[UnreachableMember] )

  override def postStop(): Unit = cluster.unsubscribe(self)

  private var state = QuoteState()
  private var featuredQuote: Quote = Quote(0,"")
  private var lastQuoteTime: Long = 0

  override def receive: Receive = {
    case message: String =>
      println("$$$$Message recieved: "+message+" from: "+ sender() )
    case CreateQuote( body ) =>
//      persist( Created(body) ) { evt =>
      state = state.created(body)
      sender ! "done"
//      }
    case GetQuotes =>
      sender ! state.quotes
    case EraseQuote( id ) =>
      if( !state.quotes.exists( q=> q.id == id ) ){
        sender ! QuoteActionPerformed(s"no such quote!")
      }else {
//        persist( Erased(id) ) { evt =>
        state = state.erased(id)
        sender ! QuoteActionPerformed(s"successfull")
//        }
      }
    case ChangeQuote(id,body) =>
      if( !state.quotes.exists( q => q.id == id ) ){
        sender ! QuoteActionPerformed(s"no such quote!")
      }else{
//        persist( Changed(id,body) ){ evt =>
        state=state.changed(id,body)
        sender ! QuoteActionPerformed(s"OK")
//        }
      }
    case FeaturedQuote =>
      val now=System.currentTimeMillis()
      val rng=scala.util.Random
      if( now - lastQuoteTime > 60000 ){
        lastQuoteTime=now
        val index= rng.nextInt(state.quotes.size)
        featuredQuote=state.quotes(index)
      }
      sender ! featuredQuote
  }

//  override def receiveRecover: Receive = {
//    case Created(body) =>
//      println("recover created quote")
//      state=state.created(body)
//    case Erased(id) =>
//      println("recover erased quote")
//      state=state.erased(id)
//    case Changed(id,body) =>
//      println("recover changed quote")
//      state=state.changed(id,body)
//  }

}