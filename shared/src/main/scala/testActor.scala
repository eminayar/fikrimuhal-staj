import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ShardRegion

import scala.concurrent.duration._

object testActor{
  val idExtractor: ShardRegion.ExtractEntityId = {
    case msg @ ShardMessage(id,_) => (id.toString,msg)
  }
  val shardResolver: ShardRegion.ExtractShardId = {
    case ShardMessage(id,_) => math.abs(id.hashCode()%5).toString
  }
}

class testActor extends Actor with ActorLogging{
  context.setReceiveTimeout(120.seconds)

  override def receive: Receive = {
    case ShardMessage(id,message) =>
      log.info("message recieved=>id: "+id+" message: "+message)
  }
}
