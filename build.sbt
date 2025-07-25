import scalapb.compiler.Version.scalapbVersion
import scalajsbundler.util.JSON._
import sbtcrossproject.{crossProject, CrossType}

lazy val Version = new {
  val scala213Versions = Seq(
    "2.13.11",
    "2.13.12",
    "2.13.13",
    "2.13.14",
    "2.13.15",
    "2.13.16"
  )
  val scala212Versions = Seq(
    "2.12.16",
    "2.12.17",
    "2.12.18",
    "2.12.19",
    "2.12.20"
  )
  def scala213 = scala213Versions.last
  def scala212 = scala212Versions.last

  def mtags = "1.3.5"
  // Important: this should be the exact same version as the one mtags pulls, as mtags uses some scalameta internal APIs,
  // and binary compatibility of these APIs isn't guaranteed.
  // Get this version with a command like 'cs resolve org.scalameta:mtags_2.13.14:1.3.1 | grep org.scalameta:scalameta'
  def scalameta = "4.9.6"
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
    scalaVersion := Version.scala213,
    crossScalaVersions := Seq(
      Version.scala213,
      Version.scala212
    ),
    scalacOptions := Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked"
    )
  )
)

(Global / cancelable) := true

(publish / skip) := true
crossScalaVersions := Nil

lazy val fullCrossVersionSettings = Def.settings(
  crossVersion := CrossVersion.full,
  crossScalaVersions := Version.scala213Versions ++ Version.scala212Versions
)

def addPaigesLikeSourceDirs(config: Configuration, srcName: String) =
  Def.settings(
    config / unmanagedSourceDirectories ++= {
      val srcBaseDir = baseDirectory.value
      val scalaVersion0 = scalaVersion.value
      def extraDirs(suffix: String) =
        List(srcBaseDir / "src" / srcName / s"scala$suffix")
      CrossVersion.partialVersion(scalaVersion0) match {
        case Some((2, y)) if y <= 12 =>
          extraDirs("-2.12-")
        case Some((2, y)) if y >= 13 =>
          extraDirs("-2.13+")
        case Some((3, _)) =>
          extraDirs("-2.13+")
        case _ => Nil
      }
    }
  )

lazy val example = project
  .in(file("paiges") / "core")
  .settings(
    publish / skip := true,
    addPaigesLikeSourceDirs(Compile, "main"),
    addCompilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % Version.scalameta cross CrossVersion.full
    ),
    scalacOptions ++= Seq(
      "-Yrangepos",
      "-Xplugin-require:semanticdb"
    ),
    libraryDependencies ++= List(
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scalacheck" %% "scalacheck" % "1.18.1" % Test,
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0" % Test
    ),
    test := {} // no need to run paiges tests.
  )

lazy val server = project
  .in(file("metabrowse-server"))
  .settings(
    moduleName := "metabrowse-server",
    fullCrossVersionSettings,
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= List(
      "io.undertow" % "undertow-core" % "2.0.30.Final",
      "org.slf4j" % "slf4j-api" % "2.0.17",
      "org.jboss.xnio" % "xnio-nio" % "3.8.0.Final",
      "org.scalameta" % "semanticdb-scalac-core" % Version.scalameta cross CrossVersion.full,
      ("org.scalameta" %% "mtags" % Version.mtags).cross(CrossVersion.full),
      "com.lihaoyi" %% "os-lib" % "0.11.4"
    ),
    (Compile / packageBin) := {
      import java.io.FileOutputStream
      import java.nio.file.attribute.FileTime
      import java.nio.file.Files
      import java.util.zip._
      import scala.collection.JavaConverters._
      val base = (Compile / packageBin).value
      val updated =
        base.getParentFile / s"${base.getName.stripSuffix(".jar")}-with-resources.jar"

      val fos = new FileOutputStream(updated)
      val zos = new ZipOutputStream(fos)

      val zf = new ZipFile(base)
      val buf = Array.ofDim[Byte](64 * 1024)
      for (ent <- zf.entries.asScala) {
        zos.putNextEntry(ent)
        val is = zf.getInputStream(ent)
        var read = -1
        while ({
          read = is.read(buf)
          read >= 0
        }) {
          if (read > 0)
            zos.write(buf, 0, read)
        }
      }
      zf.close()

      // FIXME - use fullOptJS: https://github.com/scalameta/metabrowse/issues/271
      val _ = (js / Compile / fastOptJS / webpack).value
      val targetDir = (js / Compile / npmUpdate).value
      // scalajs-bundler does not support setting a custom output path so
      // explicitly include only those files that are generated by webpack.
      val includes: FileFilter =
        "index.html" | "metabrowse.*.css" | "*-bundle.js" | "favicon.png"
      val paths: PathFinder =
        (
          targetDir./("assets").allPaths +++
            targetDir./("vs").allPaths +++
            targetDir.*(includes)
        ) --- targetDir
      val mappings = paths.get pair Path.relativeTo(targetDir)
      val prefix = "metabrowse/server/assets/"
      for ((f, path) <- mappings) {
        if (f.isDirectory) {
          val ent = new ZipEntry(prefix + path.stripSuffix("/") + "/")
          ent.setLastModifiedTime(FileTime.fromMillis(f.lastModified()))
          zos.putNextEntry(ent)
        } else {
          val ent = new ZipEntry(prefix + path)
          ent.setLastModifiedTime(FileTime.fromMillis(f.lastModified()))
          zos.putNextEntry(ent)
          val b = Files.readAllBytes(f.toPath)
          zos.write(b)
        }
      }

      zos.finish()
      zos.close()
      fos.close()

      updated
    }
  )
  .dependsOn(coreJVM)

lazy val cli = project
  .in(file("metabrowse-cli"))
  .settings(
    moduleName := "metabrowse-cli",
    fullCrossVersionSettings,
    (assembly / mainClass) := Some("metabrowse.cli.MetabrowseCli"),
    (assembly / assemblyJarName) := "metabrowse.jar",
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaBinaryVersion.value) match {
        case Some((2, 11)) =>
          Seq("-Xexperimental")
        case _ =>
          Nil
      }
    },
    libraryDependencies ++= List(
      "com.thesamet.scalapb" %% "scalapb-json4s" % "0.12.1",
      "com.github.alexarchambault" %% "case-app" % "2.0.6",
      "com.github.pathikrit" %% "better-files" % "3.9.2"
    ),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, major)) if major >= 13 =>
          Seq(
            "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0"
          )
        case _ =>
          Seq()
      }
    }
  )
  .dependsOn(server)

lazy val js = project
  .in(file("metabrowse-js"))
  .settings(
    (publish / skip) := true,
    moduleName := "metabrowse-js",
    addPaigesLikeSourceDirs(Test, "test"),
    Compile / additionalNpmConfig := Map("private" -> bool(true)),
    Test / additionalNpmConfig := (Compile / additionalNpmConfig).value,
    scalaJSUseMainModuleInitializer := true,
    webpack / version := "4.20.2",
    startWebpackDevServer / version := "3.11.2",
    useYarn := true,
    // FIXME - use fullOptJS: https://github.com/scalameta/metabrowse/issues/271
    Compile / fastOptJS / webpackExtraArgs ++= Seq(
      "-p",
      "--mode",
      "production"
    ),
    webpackConfigFile := Some(baseDirectory.value / "webpack.config.js"),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.8",
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test
    ),
    (Compile / npmDevDependencies) ++= Seq(
      "clean-webpack-plugin" -> "3.0.0",
      "copy-webpack-plugin" -> "4.6.0",
      "css-loader" -> "0.28.11",
      "mini-css-extract-plugin" -> "0.4.3",
      "file-loader" -> "1.1.11",
      "html-webpack-plugin" -> "3.2.0",
      "image-webpack-loader" -> "4.6.0",
      "style-loader" -> "0.23.0",
      "ts-loader" -> "5.2.1",
      "typescript" -> "2.6.2",
      "webpack-merge" -> "4.2.2"
    ),
    (Compile / npmDependencies) ++= Seq(
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
  .jsSettings(
    (publish / skip) := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
  )
  .settings(
    moduleName := "metabrowse-core",
    (Compile / PB.targets) := Seq(
      scalapb.gen(
        flatPackage = true // Don't append filename to package
      ) -> (Compile / sourceManaged).value./("protobuf")
    ),
    (Compile / PB.protoSources) := Seq(
      // necessary workaround for crossProjects.
      baseDirectory.value./("../src/main/protobuf")
    ),
    libraryDependencies ++= List(
      "org.scalameta" %%% "scalameta" % Version.scalameta,
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapbVersion % "protobuf"
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
    (example / Compile / classDirectory).value,
    (example / Test / classDirectory).value
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
    scalaVersion := Version.scala212,
    crossScalaVersions := Seq(Version.scala212),
    publishLocal := publishLocal
      .dependsOn((coreJVM / publishLocal))
      .dependsOn((cli / publishLocal))
      .value,
    sbt.Keys.sbtPlugin := true,
    // scriptedBufferLog := false,
    scriptedLaunchOpts += "-Dproject.version=" + version.value,
    buildInfoPackage := "metabrowse.sbt",
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      "scalametaVersion" -> Version.scalameta,
      (coreJVM / scalaVersion),
      (coreJVM / scalaBinaryVersion)
    )
  )
  .enablePlugins(ScriptedPlugin)

lazy val tests = project
  .in(file("metabrowse-tests"))
  .configs(IntegrationTest)
  .settings(
    fullCrossVersionSettings,
    (publish / skip) := true,
    Defaults.itSettings,
    run / baseDirectory := (ThisBuild / baseDirectory).value,
    buildInfoPackage := "metabrowse.tests",
    (Test / compile / compileInputs) :=
      (Test / compile / compileInputs)
        .dependsOn(
          (example / Compile / compile),
          (example / Test / compile)
        )
        .value,
    libraryDependencies ++= List(
      "org.scalameta" %% "testkit" % Version.scalameta,
      "org.scalameta" % "semanticdb-scalac-core" % Version.scalameta cross CrossVersion.full,
      "org.scalatest" %% "scalatest" % "3.2.19",
      "org.scalacheck" %% "scalacheck" % "1.18.1",
      "org.seleniumhq.selenium" % "selenium-java" % "4.33.0" % IntegrationTest,
      "org.slf4j" % "slf4j-simple" % "2.0.17"
    ),
    (IntegrationTest / compile) := {
      _root_.io.github.bonigarcia.wdm.WebDriverManager.chromedriver.setup()
      (IntegrationTest / compile).value
    },
    buildInfoKeys := Seq[BuildInfoKey](
      "sourceroot" -> (ThisBuild / baseDirectory).value,
      "exampleClassDirectory" -> List(
        (example / Compile / classDirectory).value,
        (example / Test / classDirectory).value
      )
    ),
    (Test / fork) := true
  )
  .dependsOn(cli, server)
  .enablePlugins(BuildInfoPlugin)

commands += Command.command("ci-test") { s =>
  s"++${sys.env("SCALA_VERSION")}" ::
    "Test / compile" ::
    "metabrowse-site" ::
    "test" ::
    s
}
