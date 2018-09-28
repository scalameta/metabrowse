import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object SemanticdbCommandPlugin extends AutoPlugin {
  override def requires: Plugins = JvmPlugin
  override def trigger: PluginTrigger = allRequirements
  val ScalametaVersion = sys.env.getOrElse("SCALAMETA_VERSION", "4.0.0-M11")
  val V: Map[(Int, Int), String] = Map(
    (2 -> 11) -> "2.11.12",
    (2 -> 12) -> "2.12.6"
  )
  val isOkProject: Map[String, String => Boolean] = Map(
  ).withDefaultValue(_ => true)
  def relevantProjects(state: State): Seq[(ProjectRef, String)] = {
    val extracted = Project.extract(state)
    val root = new File(".").getAbsoluteFile.getParentFile
    val isOk = isOkProject(root.getName)
    for {
      p <- extracted.structure.allProjectRefs
      if !p.project.endsWith("JS") && !p.project.endsWith("Native")
      if (isOk(p.project))
      version <- scalaVersion.in(p).get(extracted.structure.data).toList
      partialVersion <- CrossVersion.partialVersion(version).toList.map {
        case (a, b) => (a.toInt, b.toInt)
      }
      fullVersion <- V.get(partialVersion).toList
    } yield p -> fullVersion
  }

  val root = new File(".").getAbsoluteFile.getParentFile
  val sourceroot = root.getParentFile

  val mbrowseCompile = taskKey[Unit]("compile all projects in test+compile configs")
  override def globalSettings = List(
    aggregate.in(mbrowseCompile) := false,
    mbrowseCompile := Def.taskDyn {
      val refs = relevantProjects(state.value).map(_._1)
      refs.foreach(ref => println(ref.project))
      val filter = ScopeFilter(
        projects = inProjects(refs: _*),
        configurations = inConfigurations(Compile, Test))
      compile.all(filter)
    }.value,
    commands += Command.command("mbrowse") { s =>
      val extracted = Project.extract(s)
      val settings: Seq[Setting[_]] = for {
        (p, fullVersion) <- relevantProjects(s)
        setting <- List(
          scalaVersion.in(p) := fullVersion,
          scalacOptions.in(p) += "-Yrangepos",
          scalacOptions.in(p) += "-P:semanticdb:text:on",
          scalacOptions.in(p) += "-P:semanticdb:synthetics:on",
          scalacOptions.in(p) += s"-P:semanticdb:sourceroot:$sourceroot",
          libraryDependencies.in(p) += compilerPlugin(
            "org.scalameta" % "semanticdb-scalac" % ScalametaVersion cross CrossVersion.full)
        )
      } yield setting
      val installed = extracted.append(settings, s)
      "mbrowseCompile" ::
        installed
    }
  )
}
