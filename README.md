# Description (sample taken from [akka-samples](https://github.com/akka/akka-samples/tree/2.5/akka-sample-fsm-scala))

This sample is an adaptation of [Dining Hakkers](http://www.dalnefre.com/wp/2010/08/dining-philosophers-in-humus/). It illustrates how state and behavior can be managed within an Actor the `FSM` trait.

## Dining Hakkers with FSM

Open [DiningHakkersOnFsm.scala](src/main/scala/sample/fsm/DiningHakkersOnFsm.scala).

It illustrates how the states and transitions can be defined with the `akka.actor.FSM` trait.

Start the application by typing `sbt "runMain sample.fsm.DiningHakkersOnFsm"`. In the log output you can see the actions of the `Hakker` actors.

Read more about `akka.actor.FSM` in [the documentation](http://doc.akka.io/docs/akka/2.5/scala/fsm.html).

# To run [ldfi-akka](https://github.com/KTH/ldfi-akka)

1. Checkout fresh branch

	`git checkout -b <branch_name>`

2. Clone ldfi-akka to branch root

	`git clone https://github.com/KTH/ldfi-akka.git`

3. Add the following dependency to build.sbt

	```
	lazy val ldfiakka = (project in file ("ldfi-akka"))
	.settings(
		name := "ldfi-akka",
		mainClass in Compile := Some("ldfi.akka.Main"))
	.dependsOn(global)
	```
4. Compile project

	`sbt compile`

5. Copy code in to ldfi-akka

	`sbt "ldfiakka/runMain ldfi.akka.Main --copy src/main/scala"`

6. Compile ldfi-akka

	`(cd ldfi-akka; sbt compile)`

7. Rewrite code

	`sbt "ldfiakka/runMain ldfi.akka.Main --rewrite"`

6. Compile ldfi-akka

	`(cd ldfi-akka; sbt compile)`

9. Run ldfi-akka

	`sbt "ldfiakka/runMain ldfi.akka.Main -m src/main/scala/sample/fsm/DiningHakkersOnFsm.scala -v src/main/scala/sample/fsm/DiningHakkersOnFsm.scala verifyCorrectness"`