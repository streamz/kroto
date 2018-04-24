object Dependencies {
  object kroto {
    import sbt._
    val deps = Seq(
      "org.jgroups" % "jgroups" % "4.0.11.Final",
      "org.slf4j" % "slf4j-api" % "1.7.12",
      "org.specs2" %% "specs2-core" % "2.4.13" % "test"
    )
  }
}
