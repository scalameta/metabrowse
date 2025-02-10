addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.20.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.16.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.9.2")

libraryDependencies ++= List(
  "io.github.bonigarcia" % "webdrivermanager" % "5.9.2",
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.17",
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
