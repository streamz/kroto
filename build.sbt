import sbt.Keys._
import sbtassembly.AssemblyKeys
import sbtassembly.MergeStrategy._

organization := "io.streamz"

scalaVersion := "2.11.8"

publishMavenStyle := true

val kroto =(project in file("kroto"))
  .settings(name := "kroto")
  .settings(libraryDependencies ++= Dependencies.kroto.deps)

val krotomain =(project in file("main"))
  .settings(name := "main")
  .settings(
    AssemblyKeys.assemblyJarName in assembly := "kroto-main.jar",
    AssemblyKeys.assemblyMergeStrategy in assembly := {
      case "logback.xml" => discard
      case "pom.xml" => discard
      case "pom.properties" => discard
      case x =>
        val oldStrategy = (AssemblyKeys.assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    })
  .dependsOn(kroto)

val root = (project in file("."))
  .settings(name := "root")
  .settings(publishArtifact := false)
  .aggregate(kroto, krotomain)
  .disablePlugins(sbtassembly.AssemblyPlugin)

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
