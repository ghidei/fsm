# Description (sample taken from [akka-samples](https://github.com/akka/akka-samples/tree/2.5/akka-sample-fsm-scala))

This sample is an adaptation of [Dining Hakkers](http://www.dalnefre.com/wp/2010/08/dining-philosophers-in-humus/). It illustrates how state and behavior can be managed within an Actor the `FSM` trait.

## Dining Hakkers with FSM

Open [DiningHakkersOnFsm.scala](src/main/scala/sample/fsm/DiningHakkersOnFsm.scala).

It illustrates how the states and transitions can be defined with the `akka.actor.FSM` trait.

Start the application by typing `sbt "runMain sample.fsm.DiningHakkersOnFsm"`. In the log output you can see the actions of the `Hakker` actors.

Read more about `akka.actor.FSM` in [the documentation](http://doc.akka.io/docs/akka/2.5/scala/fsm.html).

