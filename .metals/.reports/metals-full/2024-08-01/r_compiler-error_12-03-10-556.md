file://<WORKSPACE>/src/main/scala/InventoryFSM.scala
### java.lang.AssertionError: NoDenotation.owner

occurred in the presentation compiler.

presentation compiler configuration:
Scala version: 3.3.3
Classpath:
<HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala3-library_3/3.3.3/scala3-library_3-3.3.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.12/scala-library-2.13.12.jar [exists ]
Options:



action parameters:
uri: file://<WORKSPACE>/src/main/scala/InventoryFSM.scala
text:
```scala
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


```



#### Error stacktrace:

```
dotty.tools.dotc.core.SymDenotations$NoDenotation$.owner(SymDenotations.scala:2607)
	dotty.tools.dotc.core.SymDenotations$SymDenotation.isSelfSym(SymDenotations.scala:714)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse(ExtractSemanticDB.scala:160)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.apply(Trees.scala:1767)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.apply(Trees.scala:1767)
	dotty.tools.dotc.ast.Trees$Instance$TreeAccumulator.fold$1(Trees.scala:1633)
	dotty.tools.dotc.ast.Trees$Instance$TreeAccumulator.apply(Trees.scala:1635)
	dotty.tools.dotc.ast.Trees$Instance$TreeAccumulator.foldOver(Trees.scala:1666)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.traverseChildren(Trees.scala:1768)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse(ExtractSemanticDB.scala:281)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.apply(Trees.scala:1767)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.apply(Trees.scala:1767)
	dotty.tools.dotc.ast.Trees$Instance$TreeAccumulator.foldOver(Trees.scala:1674)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.traverseChildren(Trees.scala:1768)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse(ExtractSemanticDB.scala:281)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.apply(Trees.scala:1767)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.apply(Trees.scala:1767)
	dotty.tools.dotc.ast.Trees$Instance$TreeAccumulator.fold$1(Trees.scala:1633)
	dotty.tools.dotc.ast.Trees$Instance$TreeAccumulator.apply(Trees.scala:1635)
	dotty.tools.dotc.ast.Trees$Instance$TreeAccumulator.foldOver(Trees.scala:1672)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.traverseChildren(Trees.scala:1768)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse(ExtractSemanticDB.scala:281)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse$$anonfun$13(ExtractSemanticDB.scala:221)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse(ExtractSemanticDB.scala:221)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse$$anonfun$11(ExtractSemanticDB.scala:207)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse(ExtractSemanticDB.scala:207)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.apply(Trees.scala:1767)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.apply(Trees.scala:1767)
	dotty.tools.dotc.ast.Trees$Instance$TreeAccumulator.foldOver(Trees.scala:1725)
	dotty.tools.dotc.ast.Trees$Instance$TreeAccumulator.foldOver(Trees.scala:1639)
	dotty.tools.dotc.ast.Trees$Instance$TreeTraverser.traverseChildren(Trees.scala:1768)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse(ExtractSemanticDB.scala:181)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse$$anonfun$1(ExtractSemanticDB.scala:145)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse(ExtractSemanticDB.scala:145)
	scala.meta.internal.pc.SemanticdbTextDocumentProvider.textDocument(SemanticdbTextDocumentProvider.scala:39)
	scala.meta.internal.pc.ScalaPresentationCompiler.semanticdbTextDocument$$anonfun$1(ScalaPresentationCompiler.scala:238)
```
#### Short summary: 

java.lang.AssertionError: NoDenotation.owner