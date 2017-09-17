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
    val Metadoc = config("metadoc").hide
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

  override def projectSettings: Seq[Setting[_]] = Def.settings(
    ivyConfigurations += Metadoc,
    libraryDependencies += "org.scala-sbt" % "sbt-launch" % "1.0.0" % Metadoc,
    metadocClasspath := classpathTask.value,
    metadocScopeFilter := ScopeFilter(
      metadocProjectFilter.value,
      metadocConfigurationFilter.value
    ),
    metadocProjectFilter := inAnyProject,
    metadocConfigurationFilter := inConfigurations(Compile, Test),
    target in metadoc := target.value / "metadoc",
    metadoc := {
      val bootProperties = target.value / "metadoc.boot.properties"
      val bootClasspath =
        Classpaths.managedJars(Metadoc, classpathTypes.value, update.value)
      val forkOptions = ForkOptions(
        bootJars = Nil,
        javaHome = javaHome.value,
        connectInput = connectInput.value,
        outputStrategy = outputStrategy.value,
        runJVMOptions = javaOptions.value,
        workingDirectory = Some(baseDirectory.value),
        envVars = envVars.value
      )
      val output = (target in metadoc).value
      val classpath = metadocClasspath.value.flatten
      val classDirectories = Attributed
        .data(classpath)
        .collect {
          case entry if entry.isDirectory => entry.getAbsolutePath
        }
        .distinct
      val arguments = Seq(
        "-classpath",
        Path.makeString(Attributed.data(bootClasspath)),
        "xsbt.boot.Boot",
        s"@$bootProperties",
        "--clean-target-first",
        "--target",
        output.getAbsolutePath
      ) ++ classDirectories

      // Write the configuration that will launch the metadoc CLI application
      // http://www.scala-sbt.org/0.13/docs/Launcher-Configuration.html
      IO.write(
        bootProperties,
        s"""[scala]
           |  version: ${BuildInfo.scalaVersion}
           |[app]
           |  org: org.scalameta
           |  name: metadoc-cli
           |  version: ${BuildInfo.version}
           |  class: ${BuildInfo.mainClass.get}
           |  cross-versioned: binary
           |[repositories]
           |  local
           |  maven-central
           |[boot]
           |  directory: ${target.value}/metadoc-boot
           """.stripMargin
      )

      if (Fork.java(forkOptions, arguments) != 0)
        sys.error("Failed to run the metadoc CLI: " + arguments.mkString(" "))

      output
    }
  )
}
