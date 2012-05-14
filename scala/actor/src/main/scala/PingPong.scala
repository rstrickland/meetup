import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.Supervisor
import akka.config.Supervision._
import akka.dispatch.Dispatchers
import akka.dispatch.Dispatchers._

object PingPong extends App {
  val pingPong1 = actorOf[PingPongActor].start
  val pingPong2 = actorOf[PingPongActor].start

  val supervisorConfig = SupervisorConfig(AllForOneStrategy(List(classOf[Throwable]), 3, 1000), Nil)
  val supervisor = Supervisor(supervisorConfig)
  supervisor.link(pingPong1)
  supervisor.link(pingPong2)

  pingPong1 ! Ping
  val response = (pingPong2 ? PingWithCallbackMessage("Foo")).as[String].getOrElse("")
  println("Response: " + response)

  pingPong1 ! "Eat this!"

  Thread.sleep(1000) // give it a sec to come back to life

  pingPong1 ! Ping
  pingPong2 ! Ping

  sys.exit
}

/**
 * Define our messages using case classes
 */
case object Ping
case class PingWithCallbackMessage(msg: String)

/**
 * PingPongActor
 */
class PingPongActor extends Actor {
  override def receive = {
    case Ping => 
      println("Ping received")
    case PingWithCallbackMessage(msg) => 
      println("Ping received with message: " + msg)
      self.channel ! msg
    case _ => 
      throw new Exception("You killed me with a bad message!")
  }
  
  override def preStart = {
    println("PingPongActor starting")
  }

  override def postRestart(err: Throwable) = {
    println("PingPongActor restarted after " + err.toString)
  }
}
