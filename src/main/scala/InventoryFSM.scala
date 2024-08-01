import org.apache.pekko.actor.{Actor, ActorRef, Props}
import org.apache.pekko.actor.FSM
import org.apache.pekko.testkit.{TestKit, TestProbe}
import org.apache.pekko.actor.ActorSystem


// Definición de los estados y datos del FSM
sealed trait State
case object WaitForRequests extends State
case object ProcessRequest extends State
case object WaitForPublisher extends State
case object SoldOut extends State
case object ProcessSoldOut extends State

case class StateData(nrBooksInStore: Int, pendingRequests: Seq[BookRequest])

case class BookRequest(context: String, target: ActorRef)
case class BookReply(context: String, result: Either[String, Int])
case class BookSupply(nrBooks: Int)
case object BookSupplySoldOut
case object PublisherRequest
case object Done
case object PendingRequests

class Inventory(publisher: ActorRef) extends Actor with FSM[State, StateData] {
  startWith(WaitForRequests, StateData(0, Seq()))

  when(WaitForRequests) {
    case Event(request: BookRequest, data: StateData) =>
      val newStateData = data.copy(pendingRequests = data.pendingRequests :+ request)
      if (newStateData.nrBooksInStore > 0) {
        goto(ProcessRequest) using newStateData
      } else {
        goto(WaitForPublisher) using newStateData
      }

    case Event(PendingRequests, data: StateData) =>
      if (data.pendingRequests.isEmpty) stay
      else if (data.nrBooksInStore > 0) goto(ProcessRequest) using data
      else goto(WaitForPublisher) using data
  }

  when(WaitForPublisher) {
    case Event(supply: BookSupply, data: StateData) =>
      goto(ProcessRequest) using data.copy(nrBooksInStore = supply.nrBooks)

    case Event(BookSupplySoldOut, _) =>
      goto(ProcessSoldOut)
  }

  when(ProcessRequest) {
    case Event(Done, data: StateData) =>
      goto(WaitForRequests) using data.copy(
        nrBooksInStore = data.nrBooksInStore - 1,
        pendingRequests = data.pendingRequests.tail
      )
  }

  when(SoldOut) {
    case Event(request: BookRequest, _) =>
      goto(ProcessSoldOut) using StateData(0, Seq(request))
  }

  when(ProcessSoldOut) {
    case Event(Done, _) =>
      goto(SoldOut) using StateData(0, Seq())
  }

  onTransition {
    case _ -> WaitForRequests =>
      if (!nextStateData.pendingRequests.isEmpty) self ! PendingRequests

    case _ -> WaitForPublisher =>
      publisher ! PublisherRequest

    case _ -> ProcessRequest =>
      val request = nextStateData.pendingRequests.head
      // Assuming reserveId is defined somewhere
      request.target ! BookReply(request.context, Right(1))
      self ! Done

    case _ -> ProcessSoldOut =>
      nextStateData.pendingRequests.foreach { request =>
        request.target ! BookReply(request.context, Left("SoldOut"))
      }
      self ! Done
  }

  initialize()
}

class Publisher(totalNrBooks: Int, nrBooksPerRequest: Int) extends Actor {
  var nrLeft = totalNrBooks

  def receive = {
    case PublisherRequest =>
      if (nrLeft == 0)
        sender ! BookSupplySoldOut
      else {
        val supply = math.min(nrBooksPerRequest, nrLeft)
        nrLeft -= supply
        sender ! BookSupply(supply)
      }
  }
}

