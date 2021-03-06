name := "Akka Test"

version := "1.0"

organization := "rstrickland"

scalaVersion := "2.9.1"

scalaSource in Compile <<= baseDirectory(_ / "src")

scalaSource in Test <<= baseDirectory(_ / "src/test")

scalacOptions += "-deprecation"

parallelExecution in Test := true

libraryDependencies ++= Seq(
  "se.scalablesolutions.akka" % "akka-actor" % "1.2",
  "se.scalablesolutions.akka" % "akka-http" % "1.2",
  "org.eclipse.jetty" % "jetty-webapp" % "8.0.0.M2" % "test",
  "org.mortbay.jetty" % "servlet-api" % "3.0.20100224" % "provided"
)

