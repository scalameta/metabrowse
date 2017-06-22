import com.trueaccord.scalapb.compiler.Version.scalapbVersion

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
      <developer>
        <id>jonas</id>
        <name>Jonas Fonseca</name>
        <url>https://github.com/jonas</url>
      </developer>
    </developers>
)

lazy val example = project

lazy val protobufSettings = Seq(
  PB.targets.in(Compile) := Seq(
    scalapb.gen(
      flatPackage = true // Don't append filename to package
    ) -> sourceManaged.in(Compile).value
  ),
  libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % scalapbVersion
)

lazy val cli = project
  .in(file("metadoc-cli"))
  .settings(
    allSettings,
    protobufSettings,
    moduleName := "metadoc-cli",
    mainClass.in(assembly) := Some("metadoc.cli.MetadocCli"),
    assemblyJarName.in(assembly) := "metadoc.jar",
    libraryDependencies ++= List(
      "org.scalameta" %% "scalameta" % "1.8.0",
      "com.github.alexarchambault" %% "case-app" % "1.2.0-M3",
      "com.github.pathikrit" %% "better-files" % "3.0.0"
    ),
    javaOptions := Nil,
    test.in(Test) :=
      test.in(Test).dependsOn(compile.in(example, Compile)).value,
    buildInfoPackage := "metadoc.tests",
    buildInfoKeys := Seq[BuildInfoKey](
      "exampleClassDirectory" -> classDirectory.in(example, Compile).value
    )
  )
  .enablePlugins(BuildInfoPlugin)

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
      "copy-webpack-plugin" -> "4.0.1",
      "css-loader" -> "0.28.4",
      "extract-text-webpack-plugin" -> "2.1.2",
      "file-loader" -> "0.11.2",
      "html-webpack-plugin" -> "2.28.0",
      "image-webpack-loader" -> "3.3.1",
      "node-sass" -> "4.5.3",
      "sass-loader" -> "6.0.6",
      "style-loader" -> "0.18.2",
      "ts-loader" -> "2.1.0",
      "typescript" -> "2.3.4",
      "webpack-merge" -> "4.1.0"
    ),
    npmDependencies in Compile ++= Seq(
      "monaco-editor" -> "0.8.3",
      "roboto-fontface" -> "0.7.0"
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
