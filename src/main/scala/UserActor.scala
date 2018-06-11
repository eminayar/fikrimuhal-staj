import akka.actor._
import java.time.Instant
import akka.persistence._
import akka.persistence.journal._
import akka.persistence.snapshot._

import akka.persistence.{Persistence, PersistentActor}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}


final case class User(username: String, password: String)

final case class UserState(users: List[User] = Nil ){
  def created( user:User ): UserState = copy( user :: users )
  override def toString: String = users.reverse.toString
}

object UserActor{
  case class CreateUser(user: User)
  case class Created(user: User)
  case object ShutDown
  case object GetUsers
  case class Login(user: User)
  case class Logout(token: String)
  case class isValidToken(token: String)
  def props: Props = Props(new UserActor)
}

class UserActor extends PersistentActor{
  import UserActor._

  override def persistenceId: String = "user-actor-id-1"

  private var state = UserState()
  private var activeTokens = Set.empty[String]
  private final val secret="topsecret"

  override def receiveCommand: Receive = {
    case CreateUser(user) =>
      println("create")
      persist(Created(user)){ evt =>
        state=state.created(user)
        sender ! "Created"
      }
    case GetUsers =>
      sender ! state.users
    case ShutDown =>
      context.stop(self)
    case SaveSnapshotSuccess(metadata) =>
      println("snapshot success")
    case SaveSnapshotFailure(metadata,reason) =>
      println("snapshot failed:"+reason.toString)
    case Login(user) =>
      if( state.users.contains(user) ){
        val claim = JwtClaim(
          expiration = Some(Instant.now.plusSeconds(2700).getEpochSecond),
          issuedAt = Some(Instant.now.getEpochSecond),
          content = user.username
        )
        val token=JwtCirce.encode( claim , secret , JwtAlgorithm.HS256 )
        activeTokens+=token
        sender ! token
      }else{
        sender ! "invalid credentials"
      }
    case Logout(token) =>
      activeTokens-=token
      sender ! "OK"
    case isValidToken(token) =>
      val decoded = JwtCirce.decode( token, secret , Seq(JwtAlgorithm.HS256) )
      if( decoded.isFailure ){
        sender ! false
      }else{
        if( decoded.get.expiration.get < Instant.now.getEpochSecond ){
          activeTokens -= token
        }
        sender ! activeTokens.contains(token)
      }
  }

  override def receiveRecover: Receive = {
    case Created(user) =>
      println("recover")
      state=state.created(user)
    case SnapshotOffer( metadata , offered: UserState ) =>
      state = offered
  }

}
