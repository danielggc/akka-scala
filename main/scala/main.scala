import org.apache.pekko.actor.{Actor, ActorRef, Props}
import org.apache.pekko.actor.FSM
import org.apache.pekko.testkit.{TestKit, TestProbe}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.util.Timeout
import scala.concurrent.duration._

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("InventoryFSMTest")
  implicit val timeout: Timeout = Timeout(5.seconds) // For `ask` pattern
  import system.dispatcher

  // Create the actors
  val publisher = system.actorOf(Props(new Publisher(2, 2)), "publisher")
  val inventory = system.actorOf(Props(new Inventory(publisher)), "inventory")

  // Create a test probe
  val replyProbe = system.actorOf(Props(new Actor {
    def receive: Receive = {
      case msg => println(s"Received message: $msg")
    }
  }), "replyProbe")

  // Send messages to the Inventory actor
  inventory ! BookRequest("context1", replyProbe)
  inventory ! BookRequest("context2", replyProbe)

  // Wait for a bit to allow processing
  Thread.sleep(2000)

  // Shut down the actor system
  system.terminate()
}