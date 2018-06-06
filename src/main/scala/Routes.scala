import UserActor._
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
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
  lazy val log = Logging(system, classOf[Routes] )

  def userActor: ActorRef

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
              val userCreated: Future[ActionPerformed] = (userActor ? CreateUser(User(username,password))).mapTo[ActionPerformed]
              onSuccess(userCreated){ performed =>
                complete((StatusCodes.Created,performed.toString))
              }
            }
          }
        }
      }

    )
}
