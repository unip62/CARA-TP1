package miage.fa.cara

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._


object Router {

  final case class RouteLine(line: String)

  case object GetFullMap

}

class Router extends Actor with ActorLogging {
  var arrayOfCountersRef = new Array[ActorRef](3)
  val system = ActorSystem("Main")
  var occurencesByWord = collection.mutable.Map[String, Int]().withDefaultValue(0)
  var cmp = 0
  implicit val timeout: Timeout = 5 seconds


  override def preStart(): Unit = {
    arrayOfCountersRef(0) = system.actorOf(Props[Counter], "Counter0")
    arrayOfCountersRef(1) = system.actorOf(Props[Counter], "Counter1")
    arrayOfCountersRef(2) = system.actorOf(Props[Counter], "Counter2")

    log.info("Counter are initialized !")
  }

  def receive = {
    case Router.RouteLine(line) =>
      arrayOfCountersRef(cmp % arrayOfCountersRef.length) ! Counter.ManageRows(line)
      cmp += 1
    case Router.GetFullMap =>
      arrayOfCountersRef.foreach(actor => {
        val result: mutable.Map[String, Int] = askResultToCounter(actor)
        occurencesByWord = mergeMapWithSumOfValue(result)
      })
    sender ! occurencesByWord
  }

  private def askResultToCounter(actor: ActorRef) = {
    val future = actor ? Counter.GetOccurrences
    val result = Await.result(future, timeout.duration).asInstanceOf[mutable.Map[String, Int]]
    result
  }

  private def mergeMapWithSumOfValue(result: mutable.Map[String, Int]) = {
    occurencesByWord ++ result.map { case (k, v) => k -> (v + occurencesByWord.getOrElse(k, 0)) }
  }
}