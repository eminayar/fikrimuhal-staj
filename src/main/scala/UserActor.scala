import akka.actor.{Actor, ActorLogging, Props}

final case class User(username: String, password: String)
final case class Users(users: Seq[User])

object UserActor {
  final case class ActionPerformed( description: String )
  final case class CreateUser(user: User)
  final case object GetUsers

  def props: Props = Props(new UserActor)
}

class UserActor extends Actor with ActorLogging{
  import UserActor._

  var users = Set.empty[User]

  override def receive: Receive = {

    case CreateUser(user) =>
      users += user
      sender ! ActionPerformed(s"User ${user.username} created.")

    case GetUsers =>
      sender ! Users(users.toSeq)
  }

}
