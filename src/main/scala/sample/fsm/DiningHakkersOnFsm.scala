package sample.fsm

import akka.actor._
import akka.actor.FSM._
import akka.event.LoggingReceive

import scala.collection.mutable
import scala.concurrent.duration._

// Akka adaptation of
// http://www.dalnefre.com/wp/2010/08/dining-philosophers-in-humus/

/*
* Some messages for the chopstick
*/
sealed trait ChopstickMessage
object Take extends ChopstickMessage
object Put extends ChopstickMessage
final case class Taken(chopstick: ActorRef) extends ChopstickMessage
final case class Busy(chopstick: ActorRef) extends ChopstickMessage

/**
 * Some states the chopstick can be in
 */
sealed trait ChopstickState
case object Available extends ChopstickState
case object Taken extends ChopstickState

/**
 * Some state container for the chopstick
 */
final case class TakenBy(hakker: ActorRef)

/*
* A chopstick is an actor, it can be taken, and put back
*/
class Chopstick extends Actor with FSM[ChopstickState, TakenBy] {
  import context._

  // A chopstick begins its existence as available and taken by no one
  startWith(Available, TakenBy(system.deadLetters))

  // When a chopstick is available, it can be taken by a some hakker
  when(Available) {
    case Event(Take, _) =>
      sender() ! Taken(self)
      goto(Taken) using TakenBy(sender())
  }

  // When a chopstick is taken by a hakker
  // It will refuse to be taken by other hakkers
  // But the owning hakker can put it back
  when(Taken) {
    case Event(Take, currentState) =>
      sender() ! Busy(self)
      stay()
    case Event(Put, TakenBy(hakker)) if sender() == hakker =>
      goto(Available) using TakenBy(system.deadLetters)
  }

  // Initialize the chopstick
  initialize()
}

/**
 * Some fsm hakker messages
 */
sealed trait FSMHakkerMessage
object Think extends FSMHakkerMessage

/**
 * Some fsm hakker states
 */
sealed trait FSMHakkerState
case object Waiting extends FSMHakkerState
case object Thinking extends FSMHakkerState
case object Hungry extends FSMHakkerState
case object WaitForOtherChopstick extends FSMHakkerState
case object FirstChopstickDenied extends FSMHakkerState
case object Eating extends FSMHakkerState

/**
 * Some state container to keep track of which chopsticks we have
 */
final case class TakenChopsticks(left: Option[ActorRef], right: Option[ActorRef])

/*
* A fsm hakker is an awesome dude or dudette who either thinks about hacking or has to eat ;-)
*/
class FSMHakker(name: String, left: ActorRef, right: ActorRef) extends Actor with FSM[FSMHakkerState, TakenChopsticks] {

  //All hakkers start waiting
  startWith(Waiting, TakenChopsticks(None, None))

  when(Waiting) {
    case Event(Think, _) =>
      println("%s starts to think".format(name))
      startThinking(5.seconds)
  }

  //When a hakker is thinking it can become hungry
  //and try to pick up its chopsticks and eat
  when(Thinking) {
    case Event(StateTimeout, _) =>
      left ! Take
      right ! Take
      goto(Hungry)
  }

  // When a hakker is hungry it tries to pick up its chopsticks and eat
  // When it picks one up, it goes into wait for the other
  // If the hakkers first attempt at grabbing a chopstick fails,
  // it starts to wait for the response of the other grab
  when(Hungry) {
    case Event(Taken(`left`), _) =>
      goto(WaitForOtherChopstick) using TakenChopsticks(Some(left), None)
    case Event(Taken(`right`), _) =>
      goto(WaitForOtherChopstick) using TakenChopsticks(None, Some(right))
    case Event(Busy(_), _) =>
      goto(FirstChopstickDenied)
  }

  // When a hakker is waiting for the last chopstick it can either obtain it
  // and start eating, or the other chopstick was busy, and the hakker goes
  // back to think about how he should obtain his chopsticks :-)
  when(WaitForOtherChopstick) {
    case Event(Taken(`left`), TakenChopsticks(None, Some(right))) => startEating(left, right)
    case Event(Taken(`right`), TakenChopsticks(Some(left), None)) => startEating(left, right)
    case Event(Busy(chopstick), TakenChopsticks(leftOption, rightOption)) =>
      leftOption.foreach(left => left ! Put)
      rightOption.foreach(right => right ! Put)
      startThinking(10.milliseconds)
  }

  private def startEating(left: ActorRef, right: ActorRef): State = {
    println("%s has picked up %s and %s and starts to eat".format(name, left.path.name, right.path.name))
    goto(Eating) using TakenChopsticks(Some(left), Some(right)) forMax (5.seconds)
  }

  // When the results of the other grab comes back,
  // he needs to put it back if he got the other one.
  // Then go back and think and try to grab the chopsticks again
  when(FirstChopstickDenied) {
    case Event(Taken(secondChopstick), _) =>
      secondChopstick ! Put
      startThinking(10.milliseconds)
    case Event(Busy(chopstick), _) =>
      startThinking(10.milliseconds)
  }

  // When a hakker is eating, he can decide to start to think,
  // then he puts down his chopsticks and stops
  when(Eating) {
    case Event(StateTimeout, _) =>
      println("%s puts down his chopsticks and starts to think".format(name))
      left ! Put
      right ! Put
      stop()
  }

  // Initialize the hakker
  initialize()

  private def startThinking(duration: FiniteDuration): State = {
    goto(Thinking) using TakenChopsticks(None, None) forMax duration
  }

}

/*
* Alright, here's our test-harness
*/
object DiningHakkersOnFsm {

  var terminatedActors: mutable.Set[ActorRef] = mutable.Set.empty

  def main(args: Array[String]): Unit = run()

  def run(): Unit = {

    val system = ActorSystem()
    terminatedActors = mutable.Set.empty

    // Create 5 chopsticks
    val chopsticks = for (i <- 1 to 5) yield system.actorOf(Props[Chopstick], "Chopstick" + i)
    // Create 5 awesome fsm hakkers and assign them their left and right chopstick

    val hakkers = for {
      (name, i) <- List("Ghosh", "Boner", "Klang", "Krasser", "Manie").zipWithIndex
    } yield system.actorOf(Props(classOf[FSMHakker], name, chopsticks(i), chopsticks((i + 1) % 5)))

    system.actorOf(Terminator.props(hakkers.toSet), "terminator")

    hakkers.foreach(hak => hak ! Think)

    //system generally terminates in 25~ seconds. Any longer than 40 seconds => something must have gone wrong
    Thread.sleep(40000)

    system.terminate()

    println(verifyCorrectness())

  }

  def verifyCorrectness(): Boolean = {
    terminatedActors.size == 5
  }

  object Terminator {
    def props(actors: Set[ActorRef]): Props = Props(new Terminator(actors))
  }

  class Terminator(actors: Set[ActorRef]) extends Actor {
    actors.foreach(act => context watch act)

    def receive = LoggingReceive {
      case Terminated(actor) =>
        terminatedActors += actor
        println("Actor " + actor.path.name + " has finished eating and has stopped.")
        if(actors == terminatedActors) println("All actors have finished eating.")
    }
  }

}


