import com.trueaccord.scalapb.compiler.Version.scalapbVersion
import scalajsbundler.util.JSON._

inThisBuild(
  List(
    organization := "org.scalameta",
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/scalameta/metadoc")),
    autoAPIMappings := true,
    apiURL := Some(url("https://scalameta.github.io/metadoc")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/scalameta/metadoc"),
        "scm:git:git@github.com:scalameta/metadoc.git"
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
)

lazy val Version = new {
  def scala = "2.12.3"
  def scala210 = "2.10.6"
  def scalameta = "2.0.0-RC1"
}

lazy val allSettings = Seq(
  scalaVersion := Version.scala,
  crossScalaVersions := Seq(Version.scala),
  scalacOptions := Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked"
  ),
  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.0.3" % Test,
    "org.scalacheck" %%% "scalacheck" % "1.13.5" % Test
  )
)

lazy val example = project
  .in(file("paiges") / "core")
  .settings(
    noPublish,
    addCompilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % Version.scalameta cross CrossVersion.full
    ),
    scalacOptions ++= Seq(
      "-Yrangepos",
      "-Xplugin-require:semanticdb"
    ),
    test := {} // no need to run paiges tests.
  )

lazy val cli = project
  .in(file("metadoc-cli"))
  .settings(
    allSettings,
    moduleName := "metadoc-cli",
    mainClass.in(assembly) := Some("metadoc.cli.MetadocCli"),
    assemblyJarName.in(assembly) := "metadoc.jar",
    libraryDependencies ++= List(
      "com.github.alexarchambault" %% "case-app" % "1.2.0-M3",
      "com.github.pathikrit" %% "better-files" % "3.0.0"
    )
  )
  .dependsOn(coreJVM)

lazy val js = project
  .in(file("metadoc-js"))
  .settings(
    noPublish,
    moduleName := "metadoc-js",
    additionalNpmConfig in Compile := Map("private" -> bool(true)),
    additionalNpmConfig in Test := additionalNpmConfig.in(Test).value,
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    scalaJSUseMainModuleInitializer := true,
    version in webpack := "2.6.1",
    version in installWebpackDevServer := "2.2.0",
    useYarn := true,
    emitSourceMaps := false, // Disabled to reduce warnings
    webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.2"
    ),
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
      "monaco-editor" -> "0.10.0",
      "roboto-fontface" -> "0.7.0",
      "js-sha512" -> "0.4.0"
    )
  )
  .dependsOn(coreJS)
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)

lazy val core = crossProject
  .crossType(CrossType.Pure)
  .in(file("metadoc-core"))
  .settings(
    allSettings,
    moduleName := "metadoc-core",
    PB.targets.in(Compile) := Seq(
      scalapb.gen(
        flatPackage = true // Don't append filename to package
      ) -> sourceManaged.in(Compile).value./("protobuf")
    ),
    PB.protoSources.in(Compile) := Seq(
      // necessary workaround for crossProjects.
      baseDirectory.value./("../src/main/protobuf")
    ),
    libraryDependencies ++= List(
      "org.scalameta" %%% "langmeta" % Version.scalameta,
      "com.trueaccord.scalapb" %%% "scalapb-runtime" % scalapbVersion
    )
  )
lazy val coreJVM = core.jvm
lazy val coreJS = core.js

commands += Command.command("metadoc-site") { s =>
  val cliRun = Array(
    "cli/run",
    "--clean-target-first",
    "--target",
    "target/metadoc",
    classDirectory.in(example, Compile).value,
    classDirectory.in(example, Test).value
  ).mkString(" ")

  "example/test:compile" ::
    cliRun ::
    s
}

val sbtPlugin = project
  .in(file("sbt-metadoc"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "sbt-metadoc",
    allSettings,
    scalaVersion := Version.scala210,
    crossScalaVersions := Seq(Version.scala210),
    publishLocal := publishLocal
      .dependsOn(publishLocal in coreJVM)
      .dependsOn(publishLocal in cli)
      .value,
    sbt.Keys.sbtPlugin := true,
    scriptedSettings,
    // scriptedBufferLog := false,
    scriptedLaunchOpts += "-Dproject.version=" + version.value,
    buildInfoPackage := "metadoc.sbt",
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      "scalametaVersion" -> Version.scalameta,
      scalaVersion.in(coreJVM),
      scalaBinaryVersion.in(coreJVM)
    )
  )

lazy val tests = project
  .in(file("metadoc-tests"))
  .settings(
    noPublish,
    buildInfoPackage := "metadoc.tests",
    buildInfoKeys := Seq[BuildInfoKey](
      "exampleClassDirectory" -> classDirectory.in(example, Compile).value
    )
  )
  .dependsOn(cli, example % Test)
  .enablePlugins(BuildInfoPlugin)

lazy val noPublish = allSettings ++ Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

noPublish
