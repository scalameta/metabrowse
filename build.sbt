import scalapb.compiler.Version.scalapbVersion
import scalajsbundler.util.JSON._
import sbtcrossproject.{crossProject, CrossType}

lazy val Version = new {
  def scala212 = "2.12.7"
  def scala211 = "2.11.12"
  def scalameta = "4.1.0"
}

inThisBuild(
  List(
    organization := "org.scalameta",
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    homepage := Some(url("https://github.com/scalameta/metabrowse")),
    autoAPIMappings := true,
    apiURL := Some(url("https://scalameta.github.io/metabrowse")),
    developers := List(
      Developer(
        "olafurpg",
        "Ólafur Páll Geirsson",
        "olafurpg@users.noreply.github.com",
        url("https://geirsson.com")
      ),
      Developer(
        "jonas",
        "Jonas Fonseca",
        "jonas@users.noreply.github.com",
        url("https://github.com/jonas")
      )
    ),
    scalaVersion := Version.scala212,
    crossScalaVersions := Seq(Version.scala212, Version.scala211),
    scalacOptions := Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked"
    )
  )
)

cancelable.in(Global) := true

skip in publish := true
crossScalaVersions := Nil

lazy val example = project
  .in(file("paiges") / "core")
  .settings(
    skip in publish := true,
    addCompilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % Version.scalameta cross CrossVersion.full
    ),
    scalacOptions ++= Seq(
      "-Yrangepos",
      "-Xplugin-require:semanticdb"
    ),
    libraryDependencies ++= List(
      "org.scalatest" %% "scalatest" % "3.0.6" % Test,
      "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
    ),
    test := {} // no need to run paiges tests.
  )

lazy val server = project
  .in(file("metabrowse-server"))
  .settings(
    moduleName := "metabrowse-server",
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= List(
      "io.undertow" % "undertow-core" % "2.0.20.Final",
      "org.slf4j" % "slf4j-api" % "1.8.0-beta4",
      "org.jboss.xnio" % "xnio-nio" % "3.7.1.Final",
      "org.scalameta" % "interactive" % "4.1.4" cross CrossVersion.full,
      "org.scalameta" %% "mtags" % "0.4.4"
    )
  )
  .dependsOn(cli)

lazy val cli = project
  .in(file("metabrowse-cli"))
  .settings(
    moduleName := "metabrowse-cli",
    mainClass.in(assembly) := Some("metabrowse.cli.MetabrowseCli"),
    assemblyJarName.in(assembly) := "metabrowse.jar",
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaBinaryVersion.value) match {
        case Some((2, 11)) =>
          Seq("-Xexperimental")
        case _ =>
          Nil
      }
    },
    libraryDependencies ++= List(
      "com.thesamet.scalapb" %% "scalapb-json4s" % "0.9.0",
      "com.github.alexarchambault" %% "case-app" % "1.2.0",
      "com.github.pathikrit" %% "better-files" % "3.7.1"
    ),
    resourceGenerators in Compile += Def.task {
      val zip = (resourceManaged in Compile).value / "metabrowse-assets.zip"
      val _ = (webpack in (js, Compile, fullOptJS)).value
      val targetDir = (npmUpdate in (js, Compile)).value
      // scalajs-bundler does not support setting a custom output path so
      // explicitly include only those files that are generated by webpack.
      val includes: FileFilter = "index.html" | "metabrowse.*.css" | "*-bundle.js" | "favicon.png"
      val paths: PathFinder =
        (
          targetDir./("assets").allPaths +++
            targetDir./("vs").allPaths +++
            targetDir.*(includes)
        ) --- targetDir
      val mappings = paths.get pair Path.relativeTo(targetDir)
      IO.zip(mappings, zip)
      Seq(zip)
    }.taskValue
  )
  .dependsOn(coreJVM)

lazy val js = project
  .in(file("metabrowse-js"))
  .settings(
    skip in publish := true,
    moduleName := "metabrowse-js",
    additionalNpmConfig in Compile := Map("private" -> bool(true)),
    additionalNpmConfig in Test := additionalNpmConfig.in(Compile).value,
    scalacOptions += "-P:scalajs:sjsDefinedByDefault",
    scalaJSUseMainModuleInitializer := true,
    version in webpack := "4.20.2",
    version in startWebpackDevServer := "3.1.8",
    useYarn := true,
    emitSourceMaps := false, // Disabled to reduce warnings
    webpackExtraArgs in (Compile, fullOptJS) ++= Seq(
      "-p",
      "--mode",
      "production"
    ),
    webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.6",
      "org.scalatest" %%% "scalatest" % "3.0.6" % Test
    ),
    npmDevDependencies in Compile ++= Seq(
      "copy-webpack-plugin" -> "4.5.2",
      "css-loader" -> "0.28.11",
      "mini-css-extract-plugin" -> "0.4.3",
      "file-loader" -> "1.1.11",
      "html-webpack-plugin" -> "3.2.0",
      "image-webpack-loader" -> "4.3.1",
      "style-loader" -> "0.23.0",
      "ts-loader" -> "5.2.1",
      "typescript" -> "2.6.2",
      "webpack-merge" -> "4.1.4"
    ),
    npmDependencies in Compile ++= Seq(
      "pako" -> "1.0.6",
      "monaco-editor" -> "0.13.1",
      "roboto-fontface" -> "0.7.0",
      "material-components-web" -> "0.21.1"
    )
  )
  .dependsOn(coreJS)
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("metabrowse-core"))
  .jsSettings(skip in publish := true)
  .settings(
    moduleName := "metabrowse-core",
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
      "org.scalameta" %%% "scalameta" % Version.scalameta,
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapbVersion
    )
  )
lazy val coreJVM = core.jvm
lazy val coreJS = core.js

commands += Command.command("metabrowse-site") { s =>
  val cliRun = Array(
    "cli/run",
    "--clean-target-first",
    "--non-interactive",
    "--target",
    "target/metabrowse",
    classDirectory.in(example, Compile).value,
    classDirectory.in(example, Test).value
  ).mkString(" ")

  "example/test:compile" ::
    cliRun ::
    s
}

val sbtPlugin = project
  .in(file("sbt-metabrowse"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "sbt-metabrowse",
    crossScalaVersions := Seq(Version.scala212),
    publishLocal := publishLocal
      .dependsOn(publishLocal in coreJVM)
      .dependsOn(publishLocal in cli)
      .value,
    sbt.Keys.sbtPlugin := true,
    // scriptedBufferLog := false,
    scriptedLaunchOpts += "-Dproject.version=" + version.value,
    buildInfoPackage := "metabrowse.sbt",
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      "scalametaVersion" -> Version.scalameta,
      scalaVersion.in(coreJVM),
      scalaBinaryVersion.in(coreJVM)
    )
  )
  .enablePlugins(ScriptedPlugin)

lazy val tests = project
  .in(file("metabrowse-tests"))
  .configs(IntegrationTest)
  .settings(
    skip in publish := true,
    Defaults.itSettings,
    buildInfoPackage := "metabrowse.tests",
    compileInputs.in(Test, compile) :=
      compileInputs
        .in(Test, compile)
        .dependsOn(
          compile.in(example, Compile),
          compile.in(example, Test)
        )
        .value,
    libraryDependencies ++= List(
      "org.scalameta" %% "testkit" % Version.scalameta,
      "org.scalameta" % "interactive" % Version.scalameta cross CrossVersion.full,
      "org.scalatest" %% "scalatest" % "3.0.6",
      "org.scalacheck" %% "scalacheck" % "1.14.0",
      "org.seleniumhq.selenium" % "selenium-java" % "3.141.59" % IntegrationTest,
      "org.slf4j" % "slf4j-simple" % "1.8.0-beta4"
    ),
    compile.in(IntegrationTest) := {
      _root_.io.github.bonigarcia.wdm.WebDriverManager.chromedriver.setup()
      compile.in(IntegrationTest).value
    },
    buildInfoKeys := Seq[BuildInfoKey](
      "exampleClassDirectory" -> List(
        classDirectory.in(example, Compile).value,
        classDirectory.in(example, Test).value
      )
    )
  )
  .dependsOn(cli, server)
  .enablePlugins(BuildInfoPlugin)

commands += Command.command("ci-test") { s =>
  s"++${sys.env("TRAVIS_SCALA_VERSION")}" ::
    "compile" ::
    "metabrowse-site" ::
    "test" ::
    s
}
