import akka.actor.{Actor, ActorLogging, Props}


final case class Quote(id: Int, body: String)
final case class Quotes(quotes: Seq[Quote])

object QuoteActor {
  final case class QuoteActionPerformed( description: String )
  final case class CreateQuote(body: String)
  final case object GetQuotes
  final case class EraseQuote(id: Int)
  final case class ChangeQuote(id: Int, body: String)
  final case object FeaturedQuote

  def props: Props = Props(new QuoteActor)
}

class QuoteActor extends Actor with ActorLogging{
  import QuoteActor._

  private var quotes = Set.empty[Quote]
  private var counter = 1

  private var featuredQuote: Quote = Quote(0,"")
  private var lastQuoteTime: Long = 0

  override def receive: Receive = {

    case CreateQuote(body) =>
      quotes += Quote(counter,body)
      counter += 1
      sender ! QuoteActionPerformed(s"Quote ${counter-1}:${body} created.")

    case GetQuotes =>
      sender ! Quotes(quotes.toSeq)

    case EraseQuote(id) =>
      if( quotes.find( q => q.id == id ).isEmpty ){
        sender ! QuoteActionPerformed(s"no such quote!")
      }else {
        quotes=quotes.filter( q=> q.id != id )
        sender ! QuoteActionPerformed(s"successfull")
      }

    case ChangeQuote(id,body) =>
      if( quotes.find( q => q.id == id ).isEmpty ){
        sender ! QuoteActionPerformed(s"no such quote!")
      }else {
        quotes=quotes.filter( q=> q.id != id )
        quotes+=Quote( id , body )
        sender ! QuoteActionPerformed(s"successfull")
      }

    case FeaturedQuote =>
      val now=System.currentTimeMillis()
      val rng=scala.util.Random
      if( now - lastQuoteTime > 60000 ){
        lastQuoteTime=now
        val index= rng.nextInt(quotes.size)
        featuredQuote=quotes.toSeq(index)
      }
      sender ! featuredQuote
  }

}
