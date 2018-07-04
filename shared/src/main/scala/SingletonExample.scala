import akka.actor.{Actor, ActorLogging}

object SingletonExample{
  case object End
}

class SingletonExample extends Actor with ActorLogging{
  import SingletonExample._
  override def receive: Receive = {
    case End =>
      context stop self
    case message: String =>
      log.info(message)
  }
}