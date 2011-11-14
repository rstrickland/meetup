name := "Akka Test"

version := "1.0"

organization := "rstrickland"

scalaVersion := "2.9.1"

scalaSource in Compile <<= baseDirectory(_ / "src")

scalaSource in Test <<= baseDirectory(_ / "src/test")

scalacOptions += "-deprecation"

parallelExecution in Test := true

libraryDependencies ++= Seq(
  "se.scalablesolutions.akka" % "akka-actor" % "1.2"
)

