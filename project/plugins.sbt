addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.13.1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.9")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.18")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.27")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0")
addSbtPlugin("org.portable-scala" % "sbt-crossproject" % "0.6.0")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.2.2")

addSbtCoursier

libraryDependencies ++= List(
  "io.github.bonigarcia" % "webdrivermanager" % "3.6.0",
  "com.thesamet.scalapb" %% "compilerplugin-shaded" % "0.8.4",
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
)
