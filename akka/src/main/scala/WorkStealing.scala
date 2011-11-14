import akka.actor.Actor
import akka.actor.Actor._
import akka.config.Supervision._
import akka.dispatch.Dispatchers
import akka.dispatch.Dispatchers._

object WorkStealer extends App {
  // create a pool of 10 actors, giving each one an id
  val actors = for (i <- 0 to 10) yield actorOf(new WorkStealingActor(i)).start

  // fire off 1000 messages to only the first actor in the list
  for (i <- 0 to 1000) actors(1) ! "Test"

  Thread.sleep(10000)

  sys.exit
}

object WorkStealingActor {
  val dispatcher = Dispatchers.newExecutorBasedEventDrivenWorkStealingDispatcher("WorkStealing").build
}

class WorkStealingActor(id: Int) extends Actor {
  self.faultHandler = OneForOneStrategy(List(classOf[Throwable]), 5, 5000)
  self.lifeCycle = Permanent
  self.dispatcher = WorkStealingActor.dispatcher
  
  override def receive = {
    case msg:String => 
      println(id + ": " + msg + " received")
    case _ =>  //do nothing
  }
}
