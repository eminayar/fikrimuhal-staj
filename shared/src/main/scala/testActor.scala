import akka.actor.{Actor, ActorLogging, PoisonPill, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate

import scala.concurrent.duration._

object testActor{
  val idExtractor: ShardRegion.ExtractEntityId = {
    case msg @ ShardMessage(id,_) => (id.toString,msg)
  }
  val shardResolver: ShardRegion.ExtractShardId = {
    case ShardMessage(id,_) => (id%5.toLong).toString
  }
}

class testActor extends Actor with ActorLogging{
  context.setReceiveTimeout(30.seconds)

  override def preStart(): Unit = {
    super.preStart()
    log.info("test actor created!")
  }

  override def postStop(): Unit = {
    super.postStop()
    log.info("test actor stopped!")
  }

  override def receive: Receive = {
    case ShardMessage(id,message) =>
      log.info("message recieved=>id: "+id+" message: "+message)
    case ReceiveTimeout => context.parent ! Passivate(stopMessage = PoisonPill )
  }
}
