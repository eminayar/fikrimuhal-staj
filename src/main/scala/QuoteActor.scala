import akka.actor.{Actor, ActorLogging, Props}
import akka.persistence.PersistentActor


final case class Quote(id: Int, body: String)

object QuoteActor {
  final case class QuoteActionPerformed( description: String )
  final case class CreateQuote(body: String)
  final case class Created(body: String)
  final case object GetQuotes
  final case class EraseQuote(id: Int)
  final case class Erased(id: Int)
  final case class ChangeQuote(id: Int, body: String)
  final case class Changed(id: Int, body: String)
  final case object FeaturedQuote

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

class QuoteActor extends PersistentActor{
  import QuoteActor._

  override def persistenceId: String = "quote-actor-id-1"

  private var state = QuoteState()
  private var featuredQuote: Quote = Quote(0,"")
  private var lastQuoteTime: Long = 0

  override def receiveCommand: Receive = {
    case CreateQuote( body ) =>
      persist( Created(body) ) { evt =>
        state = state.created(body)
        sender ! "done"
      }
    case GetQuotes =>
      sender ! state.quotes
    case EraseQuote( id ) =>
      if( !state.quotes.exists( q=> q.id == id ) ){
        sender ! QuoteActionPerformed(s"no such quote!")
      }else {
        persist( Erased(id) ) { evt =>
          state = state.erased(id)
          sender ! QuoteActionPerformed(s"successfull")
        }
      }
    case ChangeQuote(id,body) =>
      if( !state.quotes.exists( q => q.id == id ) ){
        sender ! QuoteActionPerformed(s"no such quote!")
      }else{
        persist( Changed(id,body) ){ evt =>
          state=state.changed(id,body)
          sender ! QuoteActionPerformed(s"OK")
        }
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

  override def receiveRecover: Receive = {
    case Created(body) =>
      println("recover created quote")
      state=state.created(body)
    case Erased(id) =>
      println("recover erased quote")
      state=state.erased(id)
    case Changed(id,body) =>
      println("recover changed quote")
      state=state.changed(id,body)
  }

}