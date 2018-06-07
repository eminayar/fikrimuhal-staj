import akka.actor.{Actor, ActorLogging, Props}
import java.time.Instant
import pdi.jwt.{JwtCirce, JwtAlgorithm, JwtClaim}


final case class Quote(id: Int, body: String)
final case class Quotes(quotes: Seq[Quote])

object QuoteActor {
  final case class QuoteActionPerformed( description: String )
  final case class CreateQuote(body: String)
  final case object GetQuotes
  final case class EraseQuote(id: Int)

  def props: Props = Props(new QuoteActor)
}

class QuoteActor extends Actor with ActorLogging{
  import QuoteActor._

  var quotes = Set.empty[Quote]
  private var counter = 1

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
        quotes -= Quote(id, _)
        sender ! QuoteActionPerformed(s"successfull")
      }



  }

}
