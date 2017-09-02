package metadoc.sbt

import sbt._
import sbt.Keys._

/**
  * Generate a staic Metadoc site.
  *
  * == Usage ==
  *
  * This plugin must be explicitly enabled in the project that generates the
  * static metadoc site. To enable it add the following line
  * to your `.sbt` file:
  * {{{
  * enablePlugins(MetadocPlugin)
  * }}}
  *
  * The static site includes sources for projects that enable the semanticdb-scalac
  * compiler plugin, see http://scalameta.org/tutorial/#semanticdb-scalac.
  * To enable the compiler plugin, add the following to your projects settings
  *
  * {{{
  *   lazy val projectToIncludeSourcesForMetadocSite = project.settings(
  *     metadocSettings // important, must *appear* after scalacOptions.
  *   )
  * }}}
  *
  * By default, semantic data is read from `metadocClasspath` which is
  * automatically populated based on the various filter settings.
  */
object MetadocPlugin extends AutoPlugin {

  object autoImport {
    val Metadoc = config("metadoc")
    val metadocSettings = MetadocPlugin.metadocSettings
    val metadocScopeFilter =
      settingKey[ScopeFilter]("Control sources to be included in metadoc.")
    val metadocProjectFilter = settingKey[ScopeFilter.ProjectFilter](
      "Control projects to be included in metadoc."
    )
    val metadocConfigurationFilter =
      settingKey[ScopeFilter.ConfigurationFilter](
        "Control configurations to be included in metadoc."
      )
    val metadocClasspath =
      taskKey[Seq[Classpath]]("Class directories to be included in metadoc")
    val metadoc = taskKey[File]("Generate a static Metadoc site")
  }
  import autoImport._

  override def requires = plugins.JvmPlugin
  override def trigger: PluginTrigger = allRequirements

  lazy val metadocSettings: List[Def.Setting[_]] = List(
    addCompilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % BuildInfo.scalametaVersion cross CrossVersion.full
    ),
    scalacOptions ++= Seq(
      "-Yrangepos",
      "-Xplugin-require:semanticdb"
    )
  )

  lazy val classpathTask = Def.taskDyn {
    fullClasspath.all(metadocScopeFilter.value)
  }

  override def projectSettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += Metadoc,
    libraryDependencies ++= List(
      // Explicitly set the Scala version dependency so the resolution doesn't pick
      // up the Scala version of the project the plugin is enabled in.
      "org.scala-lang" % "scala-reflect" % BuildInfo.scalaVersion % Metadoc,
      "org.scalameta" % s"metadoc-cli_${BuildInfo.scalaBinaryVersion}" % BuildInfo.version % Metadoc
    ),
    metadocClasspath := classpathTask.value,
    metadocScopeFilter := ScopeFilter(
      metadocProjectFilter.value,
      metadocConfigurationFilter.value
    ),
    metadocProjectFilter := inAnyProject,
    metadocConfigurationFilter := inConfigurations(Compile, Test),
    target in metadoc := target.value / "metadoc",
    mainClass in Metadoc := Some("metadoc.cli.MetadocCli"),
    fullClasspath in Metadoc := Classpaths
      .managedJars(Metadoc, classpathTypes.value, update.value),
    runner in (Metadoc, run) := {
      val forkOptions = ForkOptions(
        bootJars = Nil,
        javaHome = javaHome.value,
        connectInput = connectInput.value,
        outputStrategy = outputStrategy.value,
        runJVMOptions = javaOptions.value,
        workingDirectory = Some(baseDirectory.value),
        envVars = envVars.value
      )
      new ForkRun(forkOptions)
    },
    metadoc := {
      val output = (target in metadoc).value
      val classpath = metadocClasspath.value.flatten
      val classDirectories = Attributed
        .data(classpath)
        .collect {
          case entry if entry.isDirectory => entry.getAbsolutePath
        }
        .distinct
      val options = Seq(
        "--clean-target-first",
        "--target",
        output.getAbsolutePath
      ) ++ classDirectories

      (runner in (Metadoc, run)).value.run(
        (mainClass in Metadoc).value.get,
        Attributed.data((fullClasspath in Metadoc).value),
        options,
        streams.value.log
      )

      output
    }
  )
}
