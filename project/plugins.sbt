addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.10.0")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "2.1.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.18")
addSbtPlugin(
  "io.get-coursier" % "sbt-coursier" % coursier.util.Properties.version
)
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.7.1"
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
