import java.time.Instant

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
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

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

  private var activeTokens = Set.empty[String]

  def isValid(token: String): Boolean = {
    val decoded = JwtCirce.decode( token, secret , Seq(JwtAlgorithm.HS256) )
    if( decoded.isFailure ){
      logger.info("wtf")
      false
    }else{
      if( decoded.get.expiration.get < Instant.now.getEpochSecond ){
        logger.info("expiration:"+decoded.get.expiration.get.toString)
        logger.info("now:"+Instant.now.getEpochSecond)
        activeTokens=activeTokens.filter( st => st != token )
      }
      activeTokens.contains(token)
    }
  }

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
                activeTokens+=token.description
                logger.info(activeTokens.toSeq.toString())
                complete(token.description)
              }
            }
          }
        }
      },
      pathPrefix( "logout" ) {
        pathEnd {
          get {
            parameters('token.as[String] ) { (token) =>
              activeTokens-=token
              val loggedOut: Future[UserActionPerformed] = (userActor ? Logout(token)).mapTo[UserActionPerformed]
              onSuccess(loggedOut) { answer =>
                complete(answer.description)
              }
            }
          }
        }
      },
      pathPrefix( "ping" ){
        pathEnd {
          get {
            parameters('token.as[String]) { (token) =>
              if( !isValid(token) ){
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
              if( !isValid(token) ){
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
              if( !isValid(token) ){
                complete(StatusCodes.NotAcceptable)
              }else {
                val quoteCreated: Future[QuoteActionPerformed] = (quoteActor ? EraseQuote(id) ).mapTo[QuoteActionPerformed]
                onSuccess(quoteCreated){ performed =>
                  complete((StatusCodes.OK,performed.toString))
                }
              }
            }
          }
        }
      },
      pathPrefix("changeQuote") {
        pathEnd {
          get {
            parameters('token.as[String], 'id.as[Int] , 'quote.as[String]) { (token,id,quote) =>
              if( !isValid(token) ){
                complete(StatusCodes.NotAcceptable)
              }else {
                val quoteChanged: Future[QuoteActionPerformed] = (quoteActor ? ChangeQuote(id,quote) ).mapTo[QuoteActionPerformed]
                onSuccess(quoteChanged){ performed =>
                  complete((StatusCodes.OK,performed.toString))
                }
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
