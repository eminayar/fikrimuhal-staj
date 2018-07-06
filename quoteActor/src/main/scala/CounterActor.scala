import akka.actor.{Actor, ActorLogging}

class CounterActor extends Actor with ActorLogging{

  private var ans: Int = 0

  override def preStart(): Unit ={
    super.preStart()
    log.info("pre-start of counter actor")
  }

  override def postStop(): Unit = {
    super.postStop()
    log.info("post-stop of counter actor")
  }

  override def receive: Receive = {
    case Count =>
      ans = ans+1
      if( ans != 5 && ans !=10 ) {
        sender ! ans
      }else if (ans == 5 ){
        throw ResumeException()
      }else{
        throw RestartException()
      }
    case Switch =>
      ans = 0
      context.become(afterRestart)
  }
  val afterRestart: Receive = {
    case Count =>
      ans = ans+1
      if( ans != 5 ){
        sender ! ans
      }else{
        throw StopException()
      }
    case Switch=>
      context.become(escalate)
  }
  val escalate: Receive = {
    case Count =>
      throw new Exception()
  }
}
