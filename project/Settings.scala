import sbt.Keys.{name, _}
import sbt.Project

object Settings {
  def global(p: Project, projectName: String): Project =
    p.settings(scalaVersion := "2.11.12")
      .settings(organization := "io.streamz")
      .settings(publishMavenStyle := true)
      .settings(name := projectName)
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
}
