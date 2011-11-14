name := "Akka-Camel Test"

version := "1.0"

organization := "rstrickland"

scalaVersion := "2.9.1"

scalaSource in Compile <<= baseDirectory(_ / "src")

scalaSource in Test <<= baseDirectory(_ / "src/test")

scalacOptions += "-deprecation"

parallelExecution in Test := true

libraryDependencies ++= Seq(
  "se.scalablesolutions.akka" % "akka-actor" % "1.2",
  "se.scalablesolutions.akka" % "akka-camel" % "1.2",
  "org.apache.camel" % "camel" % "2.8.2",
  "org.apache.camel" % "camel-jetty" % "2.8.2",
  "org.apache.camel" % "camel-mina" % "2.8.2",
  "org.apache.mina" % "mina-core" % "1.1.7",
  "org.eclipse.jetty" % "jetty-webapp" % "8.0.0.M2" % "test",
  "org.mortbay.jetty" % "servlet-api" % "3.0.20100224" % "provided"
)

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Scala Tools Nexus Snapshots" at "http://nexus.scala-tools.org/content/repositories/snapshots/",
  "Scala Tools Nexus Releases" at "http://nexus.scala-tools.org/content/repositories/releases/"
)

