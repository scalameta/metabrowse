addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.20.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.10.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7")

libraryDependencies ++= List(
  "io.github.bonigarcia" % "webdrivermanager" % "3.8.1",
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.11",
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
