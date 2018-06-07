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
import pdi.jwt.{JwtCirce, JwtAlgorithm, JwtClaim}

import scala.concurrent.duration._
import scala.io.Source

trait Routes {
  implicit def system: ActorSystem
  lazy val log = Logging(system, classOf[Routes] )

  def userActor: ActorRef
  def quoteActor: ActorRef

  implicit val timeout: Timeout= Timeout( 2.second )

  implicit val userDecoder: Decoder[User] = deriveDecoder
  implicit val userEncoder: Encoder[User] = deriveEncoder

  lazy val myRoutes: Route =
    concat(
      pathPrefix("users") {
        pathEnd {
          get {
            val users: Future[Users] = (userActor ? GetUsers).mapTo[Users]
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
              val userCreated: Future[UserActionPerformed] = (userActor ? CreateUser(User(username,password))).mapTo[UserActionPerformed]
              onSuccess(userCreated){ performed =>
                complete((StatusCodes.Created,performed.toString))
              }
            }
          }
        }
      },
      pathPrefix( "login" ) {
        pathEnd {
          get {
            parameters('username.as[String], 'password.as[String]) { (username, password) =>
              val loggedIn: Future[UserActionPerformed] = (userActor ? Login(User(username, password))).mapTo[UserActionPerformed]
              onSuccess(loggedIn) { token =>
                complete(token.description)
              }
            }
          }
        }
      },
      pathPrefix( "ping" ){
        pathEnd {
          get {
            parameters('token.as[String]) { (token) =>
              val decoded = JwtCirce.decode( token, "topsecret" , Seq(JwtAlgorithm.HS256) )
              if( decoded.isFailure){
                complete(StatusCodes.NotAcceptable)
              }else {
                val cur = System.currentTimeMillis()
                complete(cur.toString)
              }
            }
          }
        }
      },
      pathPrefix( "createQuote" ){
        pathEnd {
          get {
            parameters('token.as[String], 'quote.as[String] ) { (token,quote) =>
              val decoded = JwtCirce.decode( token, "topsecret" , Seq(JwtAlgorithm.HS256) )
              if( decoded.isFailure){
                complete(StatusCodes.NotAcceptable)
              }else {
                val quoteCreated: Future[QuoteActionPerformed] = (quoteActor ? CreateQuote(quote) ).mapTo[QuoteActionPerformed]
                onSuccess(quoteCreated){ performed =>
                  complete((StatusCodes.Created,performed.toString))
                }
              }
            }
          }
        }
      },
      pathPrefix("quotes") {
        pathEnd {
          get {
            val quotes: Future[Quotes] = (quoteActor ? GetQuotes).mapTo[Quotes]
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
              val decoded = JwtCirce.decode( token, "topsecret" , Seq(JwtAlgorithm.HS256) )
              if( decoded.isFailure){
                complete(StatusCodes.NotAcceptable)
              }else {
                val answer: Future[QuoteActionPerformed] = (quoteActor ? EraseQuote(id) ).mapTo[QuoteActionPerformed]
                onSuccess(answer){ performed =>
                  complete((StatusCodes.OK,performed.toString))
                }
              }
            }
          }
        }
      }

    )
}
