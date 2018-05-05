import sbt.Keys._
import sbtassembly.AssemblyKeys
import sbtassembly.MergeStrategy._
import Settings._

// TODO: CLEANUP

val kroto = global(project in file("kroto"), "core")
  .settings(libraryDependencies ++= Dependencies.kroto.deps)
  .disablePlugins(sbtassembly.AssemblyPlugin)

val main = global(project in file("main"), "main")
  .settings(
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % "3.3.0",
      "org.slf4j" % "slf4j-simple" % "1.7.25"))
  .settings(
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.copy(`classifier` = Some("assembly"))
    },
    addArtifact(artifact in (Compile, assembly), assembly),
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

val root = global(project in file("."), "kroto")
  .settings(publishArtifact := false)
  .aggregate(kroto, main)
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
