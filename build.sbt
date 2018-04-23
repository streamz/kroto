import sbt.Keys._

organization := "io.streamz"

name := "kroto"

scalaVersion := "2.11.8"

publishMavenStyle := true

libraryDependencies ++= {
  Seq(
    "org.jgroups" % "jgroups" % "4.0.11.Final",
    "org.slf4j" % "slf4j-api" % "1.7.12",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.specs2" %% "specs2-core" % "2.4.13" % "test"
  )
}

scalacOptions ++= Seq(
  "-language:postfixOps",
  "-deprecation",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-encoding", "UTF-8",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard"
)

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

parallelExecution in Test := false

fork := true
