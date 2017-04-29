lazy val allSettings = Seq(
  organization := "com.geirsson",
  scalaVersion := "2.12.2",
  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.0.1" % Test,
    "org.scalacheck" %%% "scalacheck" % "1.13.5" % Test
  ),
  resolvers += Resolver.bintrayRepo("scalameta", "maven"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked"
  )
)

lazy val metadoc = crossProject
  .in(file("metadoc"))
  .settings(
    allSettings,
    name := "metadoc",
    moduleName := "metadoc",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "scalameta" % "1.7.0"
    )
  )

lazy val metadocJVM = metadoc.jvm
lazy val metadocJS = metadoc.js

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

noPublish
