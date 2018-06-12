import UserActor._
import QuoteActor._
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._

import scala.concurrent.duration._

trait Routes {
  implicit def system: ActorSystem
  lazy val logger = Logging(system, classOf[Routes] )

  def userActor: ActorRef
  def quoteActor: ActorRef
  private final val secret = "topsecret"

  implicit val timeout: Timeout= Timeout( 2.second )

  implicit val userDecoder: Decoder[User] = deriveDecoder
  implicit val userEncoder: Encoder[User] = deriveEncoder

  lazy val myRoutes: Route =
    concat(
      pathPrefix("users") {
        pathEnd {
          get {
            val users: Future[List[User]] = (userActor ? GetUsers).mapTo[List[User]]
            complete{
              users.map(_.asJson.toString)
            }
          }
        }
      },
      pathPrefix("createUser"){
        pathEnd {
          get {
            parameters('username.as[String] , 'password.as[String]) { (username,password) =>
              val userCreated: Future[String] = (userActor ? CreateUser(User(username,password))).mapTo[String]
              onSuccess(userCreated){ performed =>
                complete((StatusCodes.Created,performed))
              }
            }
          }
        }
      },
      pathPrefix( "login" ) {
        pathEnd {
          get {
            parameters('username.as[String], 'password.as[String]) { (username, password) =>
              val loggedIn: Future[String] = (userActor ? Login(User(username, password))).mapTo[String]
              onSuccess(loggedIn) { token =>
                complete(token)
              }
            }
          }
        }
      },
      pathPrefix( "logout" ) {
        pathEnd {
          get {
            parameters('token.as[String] ) { (token) =>
              val loggedOut: Future[String] = (userActor ? Logout(token)).mapTo[String]
              onSuccess(loggedOut) { answer =>
                complete(answer)
              }
            }
          }
        }
      },
      pathPrefix( "ping" ){
        pathEnd {
          get {
            parameters('token.as[String]) { (token) =>
              val cur = System.currentTimeMillis()
              val answer: Future[Boolean] = (userActor ? isValidToken(token)).mapTo[Boolean]
              onSuccess(answer){ flag =>
                if( flag ){
                  complete(cur.toString)
                }else{
                  complete(StatusCodes.NotAcceptable)
                }
              }
            }
          }
        }
      },
      pathPrefix( "createQuote" ){
        pathEnd {
          get {
            parameters('token.as[String], 'quote.as[String] ) { (token,quote) =>
              val isLoggedIn: Future[Boolean] = (userActor ? isValidToken(token)).mapTo[Boolean]
              val quoteCreated = isLoggedIn map { ans =>
                if (ans) (quoteActor ? CreateQuote(quote)).mapTo[QuoteActionPerformed]
                else false
              }
              onSuccess(quoteCreated){ report =>
                if( !report.equals(false) ) complete("created!")
                else complete("not logged in!")
              }
            }
          }
        }
      },
      pathPrefix("quotes") {
        pathEnd {
          get {
            val quotes: Future[List[Quote]] = (quoteActor ? GetQuotes).mapTo[List[Quote]]
            complete{
              quotes.map(_.asJson.toString)
            }
          }
        }
      },
      pathPrefix("eraseQuote") {
        pathEnd {
          get {
            parameters('token.as[String], 'id.as[Int] ) { (token,id) =>
              val isLoggedIn: Future[Boolean] = (userActor ? isValidToken(token)).mapTo[Boolean]
              val quoteErased = isLoggedIn map{ ans =>
                if( ans ) (quoteActor ? EraseQuote(id) ).mapTo[QuoteActionPerformed]
                else false
              }
              onSuccess(quoteErased){ report =>
                if( !report.equals(false) ) complete("successfull")
                else complete("not logged in!")
              }
            }
          }
        }
      },
      pathPrefix("changeQuote") {
        pathEnd {
          get {
            parameters('token.as[String], 'id.as[Int] , 'quote.as[String]) { (token,id,quote) =>
              val isLoggedIn: Future[Boolean] = (userActor ? isValidToken(token)).mapTo[Boolean]
              val quoteErased = isLoggedIn map{ ans =>
                if( ans ) (quoteActor ? ChangeQuote(id,quote) ).mapTo[QuoteActionPerformed]
                else false
              }
              onSuccess(quoteErased){ report =>
                if( !report.equals(false) ) complete("successfull")
                else complete("not logged in!")
              }
            }
          }
        }
      },
      pathPrefix("featuredQuote") {
        pathEnd {
          get {
            val quote: Future[Quote] = (quoteActor ? FeaturedQuote).mapTo[Quote]
            complete{
              quote.map(_.asJson.toString)
            }
          }
        }
      }
    )
}
