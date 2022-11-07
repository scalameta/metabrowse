addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.20.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.0.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.10.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.2.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.11")

libraryDependencies ++= List(
  "io.github.bonigarcia" % "webdrivermanager" % "5.3.1",
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.12",
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
