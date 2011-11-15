import akka.actor.Actor
import akka.actor.Actor._
import akka.config.Supervision._
import akka.dispatch.Dispatchers
import akka.dispatch.Dispatchers._

object Simple extends App {
  val simpleActor = actorOf[SimpleActor].start

  simpleActor ! "Bang"

  val response = (simpleActor ? "Waiting").as[String].getOrElse("")
  println("Response: " + response)

  sys.exit
}

class SimpleActor extends Actor {  
  override def receive = {

    case msg:String => 
      println(msg + " received")

      if (self.channel tryTell msg + " back at you")
        println("Reply succeeded")
      else 
        println("Reply failed")

    case _ =>  //do nothing
  }
}
