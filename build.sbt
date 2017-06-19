scalaVersion in ThisBuild := "2.12.2"

lazy val allSettings = Seq(
  organization := "com.geirsson",
  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.0.3" % Test,
    "org.scalacheck" %%% "scalacheck" % "1.13.5" % Test
  ),
  resolvers += Resolver.bintrayRepo("scalameta", "maven"),
  scalacOptions := Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked"
  ),
  licenses := Seq(
    "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/olafurpg/metadoc")),
  autoAPIMappings := true,
  apiURL := Some(url("https://olafurpg.github.io/metadoc")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/scalameta/metadoc"),
      "scm:git:git@github.com:olafurpg/metadoc.git"
    )
  ),
  pomExtra :=
    <developers>
      <developer>
        <id>olafurpg</id>
        <name>Ólafur Páll Geirsson</name>
        <url>https://geirsson.com</url>
      </developer>
    </developers>
)

lazy val example = project

lazy val metadoc = crossProject
  .in(file("metadoc"))
  .settings(
    allSettings,
    name := "metadoc",
    moduleName := "metadoc",
    fork in (Test, test) := true
  )
  .configureAll(_.dependsOn(example % Scalameta))
  .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin))
  .jsSettings(
    version in webpack := "2.6.1",
    version in installWebpackDevServer := "2.2.0",
    useYarn := true,
    emitSourceMaps := false, // Disabled to reduce warnings
    webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.2",
    npmDevDependencies in Compile ++= Seq(
      "webpack-merge" -> "4.1.0",
      "html-webpack-plugin" -> "2.28.0",
      "copy-webpack-plugin" -> "4.0.1",
      "ts-loader" -> "2.1.0",
      "typescript" -> "2.3.4"
    ),
    npmDependencies in Compile += "monaco-editor" -> "0.8.3"
  )
lazy val metadocJVM = metadoc.jvm
lazy val metadocJS = metadoc.js

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

noPublish
