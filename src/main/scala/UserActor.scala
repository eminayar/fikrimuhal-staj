import akka.actor.{Actor, ActorLogging, Props}
import java.time.Instant
import pdi.jwt.{JwtCirce, JwtAlgorithm, JwtClaim}


final case class User(username: String, password: String)
final case class Users(users: Seq[User])

object UserActor {
  final case class UserActionPerformed( description: String )
  final case class CreateUser(user: User)
  final case object GetUsers
  final case class Login(user: User)

  def props: Props = Props(new UserActor)
}

class UserActor extends Actor with ActorLogging{
  import UserActor._

  var users = Set.empty[User]

  override def receive: Receive = {

    case CreateUser(user) =>
      users += user
      sender ! UserActionPerformed(s"User ${user.username} created.")

    case GetUsers =>
      sender ! Users(users.toSeq)

    case Login(user) =>
      if ( users.contains( user ) ){
        val claim = JwtClaim(
          expiration = Some(Instant.now.plusSeconds(2700).getEpochSecond),
          issuedAt = Some(Instant.now.getEpochSecond)
        )
        val token=JwtCirce.encode( claim , "topsecret" , JwtAlgorithm.HS256 )
        sender ! UserActionPerformed(token)
      }else{
        sender ! UserActionPerformed(s"invalid credentials")
      }

  }

}
