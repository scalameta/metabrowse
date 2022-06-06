addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.20.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.9")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.5")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.7.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.1.0")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.3")

libraryDependencies ++= List(
  "io.github.bonigarcia" % "webdrivermanager" % "3.6.2",
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.3",
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
