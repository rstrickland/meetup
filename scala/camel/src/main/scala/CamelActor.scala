import akka.actor.Actor
import akka.actor.Actor._
import akka.camel.{Message, Consumer, Producer, Failure, Oneway}
import akka.camel.{CamelServiceManager, CamelContextManager}

object CamelTest extends App {
  val service = CamelServiceManager.startCamelService
  val prod = actorOf[CamelProducer].start

  //service.awaitEndpointActivation(1) { actorOf[CamelConsumer].start }

  val start = System.currentTimeMillis

  //for (i <- 0 to 10)
    prod ! "sent

  val time = System.currentTimeMillis - start
  println("Message send time: " + time + " ms")

  Thread.sleep(5000)  
  
  sys.exit
}

class CamelConsumer extends Actor with Consumer {
  //def endpointUri = "vm:test" // use SEDA component
  //def endpointUri = "mina:tcp://localhost:6200" // use TCP over MINA
  //def endpointUri = "jetty:http://localhost:6200/test" // use HTTP over jetty
  def endpointUri = "xmpp://dnaticxmppenterprise@dev1.soapbox.e3smartenergy.com/?password=dnaticxmppenterprise"
  

  def receive = {
    case msg: Message => 
      val msgStr = msg.getBodyAs(classOf[String])
      println(msgStr)
      self.channel ! "ACK"
  }
}

class CamelProducer extends Actor with Producer with Oneway {
  //def endpointUri = "vm:test"
  //def endpointUri = "mina:tcp://localhost:6200"
  def endpointUri = "xmpp://dnaticxmppenterprise@dev1.soapbox.e3smartenergy.com/403a41de33cd45ff8ef5ffc36cd11001@soapbox.e3smartenergy.com/?password=dnaticxmppenterprise"
}


