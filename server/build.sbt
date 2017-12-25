name := """polylove-server"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.11" % Test,
  "org.sorm-framework" % "sorm" % "0.3.20",
  "mysql" % "mysql-connector-java" % "5.1.40",
  "org.jgrapht" % "jgrapht-core" % "1.0.0"
)

sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

jacoco.settings

parallelExecution in jacoco.Config := false

jacoco.excludes in jacoco.Config := Seq(
  "controllers.Reverse*", "controllers.routes*", "controllers.javascript*",
  "views.*", "router.*", "*Admin*"
)
