organization := "com.typesafe.akka.samples"
name := "fsm"

scalaVersion := "2.12.6"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.13"
)

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))


lazy val global = project in file (".")


