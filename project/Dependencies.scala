object Dependencies {
  object kroto {
    import sbt._
    val deps = Seq(
      "org.jgroups" % "jgroups" % "4.0.11.Final",
      "org.slf4j" % "slf4j-api" % "1.7.12",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "org.slf4j" % "slf4j-simple" % "1.7.25" % "test",
      "org.specs2" %% "specs2-core" % "2.4.13" % "test"
    )
  }
}
