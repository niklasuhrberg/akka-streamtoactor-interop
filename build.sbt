import sbt.Keys._
import sbt._

name := "stream-actor-interop"
scalaVersion := "2.13.6"
ThisBuild / version := "0.0.1"

lazy val akkaVersion = "2.6.20"



resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.jcenterRepo,
)

val commonSettings = List(
  organization := "com.triadicsystems.examples",
  scalaVersion := "2.13.6",
  publishMavenStyle := true
)


lazy val root = (project in file("."))
  .settings(
    inThisBuild(commonSettings),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
      "org.scalamock" %% "scalamock" % "4.4.0" % Test,
      "ch.qos.logback" % "logback-classic" % "1.2.9",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"
    )
  )
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.9" % "it,test"
  )

enablePlugins(JavaAppPackaging)

Compile / mainClass := Some("com.triadicsystems.examples.stageactorbased.MainStageActor")


Test / test := (Test / test).dependsOn(scalafmtCheckAll).value

