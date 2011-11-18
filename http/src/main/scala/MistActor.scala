package meetup.http

import akka.actor._
import akka.actor.Actor._
import akka.http._
import akka.config._
import akka.config.Supervision._
import akka.util._

class Boot {
  val factory = SupervisorFactory(SupervisorConfig(OneForOneStrategy(List(classOf[Exception]), 3, 100),
		  Supervise(actorOf[RootEndpoint], Permanent) :: 
                  Supervise(actorOf[MistActor], Permanent) :: Nil))
  
  factory.newInstance.start
}

class MistActor extends Actor with Endpoint {
  self.dispatcher = Endpoint.Dispatcher

  def hook(uri:String) = uri startsWith "/mistTest"
  def provide(uri:String) = actorOf[MistActorService].start

  override def preStart = registry.actorsFor(classOf[RootEndpoint]).head ! Endpoint.Attach(hook, provide)

  def receive = handleHttpRequest
}

class MistActorService extends Actor {
  def receive = {

    case get:Get =>

      def default(any: Any) = ""
      val action = get.getParameterOrElse("action", default)
      
      action match {

        case "ping" => 
          try {
            // do something interesting
            get OK "pong"            
          } catch {
            case e => get OK e.toString
          }

        case _ => get OK "Invalid parameter"
      }

    case other:RequestMethod => other NotAllowed "unsupported request"
  }
}
