package mbrowse.sbt

import sbt._
import sbt.Keys._

/**
  * Generate a staic Mbrowse site.
  *
  * == Usage ==
  *
  * This plugin must be explicitly enabled in the project that generates the
  * static mbrowse site. To enable it add the following line
  * to your `.sbt` file:
  * {{{
  * enablePlugins(MbrowsePlugin)
  * }}}
  *
  * The static site includes sources for projects that enable the semanticdb-scalac
  * compiler plugin, see http://scalameta.org/tutorial/#semanticdb-scalac.
  * To enable the compiler plugin, add the following to your projects settings
  *
  * {{{
  *   lazy val projectToIncludeSourcesForMbrowseSite = project.settings(
  *     mbrowseSettings // important, must *appear* after scalacOptions.
  *   )
  * }}}
  *
  * By default, semantic data is read from `mbrowseClasspath` which is
  * automatically populated based on the various filter settings.
  */
object MbrowsePlugin extends AutoPlugin {

  object autoImport {
    val Mbrowse = config("mbrowse")
    val mbrowseSettings = MbrowsePlugin.mbrowseSettings
    val mbrowseScopeFilter =
      settingKey[ScopeFilter]("Control sources to be included in mbrowse.")
    val mbrowseProjectFilter = settingKey[ScopeFilter.ProjectFilter](
      "Control projects to be included in mbrowse."
    )
    val mbrowseConfigurationFilter =
      settingKey[ScopeFilter.ConfigurationFilter](
        "Control configurations to be included in mbrowse."
      )
    val mbrowseClasspath =
      taskKey[Seq[Classpath]]("Class directories to be included in mbrowse")
    val mbrowse = taskKey[File]("Generate a static Mbrowse site")
  }
  import autoImport._

  override def requires = plugins.JvmPlugin
  override def trigger: PluginTrigger = allRequirements

  lazy val mbrowseSettings: List[Def.Setting[_]] = List(
    addCompilerPlugin(
      "org.scalameta" % "semanticdb-scalac" % BuildInfo.scalametaVersion cross CrossVersion.full
    ),
    scalacOptions ++= Seq(
      "-Yrangepos",
      "-Xplugin-require:semanticdb"
    )
  )

  lazy val classpathTask = Def.taskDyn {
    fullClasspath.all(mbrowseScopeFilter.value)
  }

  override def projectSettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += Mbrowse,
    libraryDependencies ++= List(
      // Explicitly set the Scala version dependency so the resolution doesn't pick
      // up the Scala version of the project the plugin is enabled in.
      "org.scala-lang" % "scala-reflect" % BuildInfo.scalaVersion % Mbrowse,
      "org.scalameta" % s"mbrowse-cli_${BuildInfo.scalaBinaryVersion}" % BuildInfo.version % Mbrowse
    ),
    mbrowseClasspath := classpathTask.value,
    mbrowseScopeFilter := ScopeFilter(
      mbrowseProjectFilter.value,
      mbrowseConfigurationFilter.value
    ),
    mbrowseProjectFilter := inAnyProject,
    mbrowseConfigurationFilter := inConfigurations(Compile, Test),
    target in mbrowse := target.value / "mbrowse",
    mainClass in Mbrowse := Some("mbrowse.cli.MbrowseCli"),
    fullClasspath in Mbrowse := Classpaths
      .managedJars(Mbrowse, classpathTypes.value, update.value),
    runner in (Mbrowse, run) := {
      new ForkRun(Compat.forkOptions.value)
    },
    mbrowse := {
      val output = (target in mbrowse).value
      val classpath = mbrowseClasspath.value.flatten
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

      (runner in (Mbrowse, run)).value.run(
        (mainClass in Mbrowse).value.get,
        Attributed.data((fullClasspath in Mbrowse).value),
        options,
        streams.value.log
      )

      output
    }
  )
}
