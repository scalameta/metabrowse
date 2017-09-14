package metadoc.cli

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.zip.{ZipEntry, ZipInputStream}
import scala.collection.mutable
import org.langmeta._
import org.langmeta.internal.io.PathIO
import caseapp.{Name => _, _}
import metadoc.{schema => d}
import better.files._
import org.langmeta.internal.inputs._
import metadoc.schema.Ranges
import org.langmeta.internal.io.FileIO

@AppName("metadoc")
@AppVersion("0.1.0-SNAPSHOT")
@ProgName("metadoc")
case class MetadocOptions(
    @HelpMessage("The output directory to generate the metadoc site.")
    target: Option[String] = None,
    @HelpMessage(
      "Clean the target directory before generating new site. " +
        "All files will be deleted so be careful."
    )
    cleanTargetFirst: Boolean = false
)

case class MetadocSite(
    semanticdb: Seq[AbsolutePath],
    symbols: Seq[d.Symbol],
    index: d.Index
)

object MetadocCli extends CaseApp[MetadocOptions] {
  def filename(input: Input): String = input match {
    case Input.VirtualFile(path, _) => path
    case Input.File(path, _) =>
      path.toRelative(PathIO.workingDirectory).toString()
    case els => els.syntax
  }

  def metadocPosition(position: Position): d.Position =
    d.Position(
      filename(position.input),
      position.start,
      position.end
    )

  def getSymbols(db: Database): Seq[d.Symbol] = {
    val symbols = mutable.Map
      .empty[String, d.Symbol]
      .withDefault(sym => d.Symbol(symbol = sym))

    def add(name: ResolvedName): Unit = {
      val syntax = name.symbol.syntax
      if (name.isDefinition) {
        symbols(syntax) = symbols(syntax).copy(
          definition = Some(metadocPosition(name.position))
        )
      } else {
        val old = symbols(syntax)
        val label = filename(name.position.input)
        val ranges = old.references.getOrElse(label, Ranges())
        val newRefences = old.references.updated(
          label,
          ranges.addRanges(d.Range(name.position.start, name.position.end))
        )
        symbols(syntax) = old.copy(references = newRefences)
      }
    }
    db.names.foreach(add)
    symbols.values.filter(_.definition.isDefined).toSeq
  }

  def encodeSymbolName(name: String): String = {
    val md = java.security.MessageDigest.getInstance("SHA-512")
    val sha = md.digest(name.getBytes("UTF-8"))
    // 512 bits ~> 64 bytes and doubled for the hex encoding
    String.format("%0128x", new java.math.BigInteger(1, sha))
  }

  def createMetadocSite(site: MetadocSite, options: MetadocOptions): Unit = {
    val target = AbsolutePath(
      options.target.getOrElse(sys.error("--target is required!"))
    )
    if (options.cleanTargetFirst && Files.exists(target.toNIO)) {
      target.toFile.toScala.delete()
    }
    def semanticdb(): Unit = {
      site.semanticdb.foreach { root =>
        val from = root.resolve("META-INF").resolve("semanticdb")
        val to = target.resolve("semanticdb")
        FileIO.listAllFilesRecursively(from).files.foreach { path =>
          val in = from.toNIO.resolve(path.toNIO)
          val out = to.resolve(path).toNIO
          Files.createDirectories(out.getParent)
          Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING)
        }
      }
    }

    def symbol(): Unit = {
      val root = target.resolve("symbol")
      root.toFile.mkdirs()
      site.symbols.foreach { symbol =>
        val url = encodeSymbolName(symbol.symbol)
        val out = root.resolve(url)
        Files.createDirectories(out.toNIO.getParent)
        Files.write(
          out.toNIO,
          symbol.toByteArray,
          StandardOpenOption.CREATE
        )
      }
    }
    def index(): Unit = {
      Files.write(
        target.resolve("metadoc.index").toNIO,
        site.index.toByteArray
      )
    }

    def assets(): Unit = {
      val root = target.toNIO
      val inputStream = MetadocCli.getClass.getClassLoader
        .getResourceAsStream("metadoc-assets.zip")
      if (inputStream == null)
        sys.error("Failed to locate metadoc-assets.zip on the classpath")
      val zipStream = new ZipInputStream(inputStream)
      val bytes = new Array[Byte](8012)

      Stream
        .continually(zipStream.getNextEntry)
        .takeWhile(_ != null)
        .filterNot(_.isDirectory)
        .foreach { entry =>
          val path = root.resolve(entry.getName)
          if (Files.notExists(path))
            Files.createDirectories(path.getParent)
          val out = Files.newOutputStream(path, StandardOpenOption.CREATE)

          def copyLoop: Unit = {
            val read = zipStream.read(bytes, 0, bytes.length)
            if (read > 0) {
              out.write(bytes, 0, read)
              copyLoop
            }
          }

          copyLoop
          out.flush()
          out.close()
        }
    }

    semanticdb()
    symbol()
    index()
    assets()
  }

  def run(options: MetadocOptions, remainingArgs: RemainingArgs): Unit = {
    val classpath = Classpath(
      remainingArgs.remainingArgs.flatMap { cp =>
        cp.split(File.pathSeparator).map(AbsolutePath(_))
      }.toList
    )
    val db = Database.load(classpath)
    val symbols = getSymbols(db)
    val files = db.documents.collect {
      case doc if doc.input.isInstanceOf[Input.VirtualFile] =>
        filename(doc.input)
    }
    val index = d.Index(files, symbols.map(_.withReferences(Map.empty)))
    val site = MetadocSite(classpath.shallow, symbols, index)
    createMetadocSite(site, options)
    println(options.target.get)
  }
}
