package metadoc.cli

import java.io.File
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import scala.collection.mutable
import scala.meta._
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import caseapp.{Name => _, _}
import metadoc.{schema => d}
import better.files._
import scala.meta.internal.ast.Helpers._

@AppName("metadoc")
@AppVersion("0.1.0-SNAPSHOT")
@ProgName("metadoc")
case class MetadocOptions(
    @HelpMessage("The output directory to generate the metadoc site.")
    target: Option[String] = None,
    @HelpMessage(
      "Clean the target directory before generating new site. " +
        "All files will be deleted so be careful.")
    cleanTargetFirst: Boolean = false
)

case class MetadocSite(semanticdb: Seq[AbsolutePath],
                       index: d.Index)

object MetadocCli extends CaseApp[MetadocOptions] {
  def filename(input: Input): String = input match {
    case Input.LabeledString(path, _) => path
    case Input.File(path, _) =>
      path.toRelative(PathIO.workingDirectory).toString()
  }

  def metadocPosition(position: Position): d.Position =
    d.Position(filename(position.input),
               start = position.start.offset,
               end = position.end.offset)

  def getAbsolutePath(path: String): AbsolutePath =
    if (PathIO.isAbsolutePath(path)) AbsolutePath(path)
    else PathIO.workingDirectory.resolve(path)

  def getSymbols(implicit db: Database): Seq[d.Symbol] = {
    val symbols = mutable.Map
      .empty[String, d.Symbol]
      .withDefault(sym => d.Symbol(symbol = sym))

    def add(name: Name): Unit = db.names.get(name.pos).foreach { symbol =>
      val syntax = symbol.syntax
      if (name.isBinder) {
        symbols(syntax) =
          symbols(syntax).copy(definition = Some(metadocPosition(name.pos)))
      } else {
        val old = symbols(syntax)
        symbols(syntax) =
          old.copy(references = old.references :+ metadocPosition(name.pos))
      }
    }

    db.sources.foreach { source =>
      source.traverse { case name: Name => add(name) }
    }
    symbols.values.iterator.filter(_.definition.isDefined).toSeq
  }

  def createMetadocSite(site: MetadocSite, options: MetadocOptions): Unit = {
    val target = getAbsolutePath(
      options.target.getOrElse(sys.error("--target is required!")))
    if (options.cleanTargetFirst && Files.exists(target.toNIO)) {
      target.toFile.toScala.delete()
    }
    def semanticdb(): Unit = {
      site.semanticdb.foreach { root =>
        val from = root.resolve("META-INF").resolve("semanticdb")
        from.toFile.toScala.copyTo(target.resolve("semanticdb").toFile.toScala)
      }
    }

    def index(): Unit = {
      Files.write(target.resolve("metadoc.index").toNIO,
                  site.index.toByteArray)
    }
    semanticdb()
    index()
  }

  def run(options: MetadocOptions, remainingArgs: RemainingArgs): Unit = {
    val classpath = Classpath(remainingArgs.remainingArgs.flatMap(cp =>
      cp.split(File.pathSeparator).map(getAbsolutePath)))
    val db = Database.load(classpath)
    val symbols = getSymbols(db)
    val files = db.entries.collect {
      case (Input.LabeledString(path, _), _) => path
    }
    val index = d.Index(files, symbols)
    val site = MetadocSite(classpath.shallow, index)
    createMetadocSite(site, options)
    println(options.target.get)
  }
}
