package metadoc.cli

import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.util.zip.{ZipEntry, ZipInputStream}
import scala.collection.mutable
import org.langmeta._
import org.langmeta.internal.io.PathIO
import caseapp.{Name => _, _}
import java.nio.file.attribute.BasicFileAttributes
import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import java.util.function.{Function => JFunction}
import java.{util => ju}
import scala.collection.GenSeq
import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal
import caseapp._
import caseapp.{Name => _}
import metadoc.schema
import metadoc.{schema => d}
import org.langmeta._
import org.langmeta.internal.semanticdb.{schema => s}

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
    cleanTargetFirst: Boolean = false,
    @HelpMessage(
      "Experimental. Emit metadoc.zip file instead of static files."
    )
    zip: Boolean = false,
    @HelpMessage("Disable fancy progress bar")
    nonInteractive: Boolean = false
)

case class Target(target: AbsolutePath, onClose: () => Unit)

class CliRunner(paths: GenSeq[AbsolutePath], options: MetadocOptions) {
  require(options.target.isDefined, "--target is required")
  type Target = (AbsolutePath, () => Unit)

  val Target(target, onClose) = if (options.zip) {
    val out = AbsolutePath(options.target.get).resolve("metadoc.zip")
    Files.createDirectories(out.toNIO.getParent)
    val zipfs = FileSystems.newFileSystem(
      URI.create(s"jar:file:${out.toURI.getPath}"), {
        val env = new util.HashMap[String, String]()
        env.put("create", "true")
        env
      }
    )
    Target(AbsolutePath(zipfs.getPath("/")), () => zipfs.close())
  } else {
    Target(AbsolutePath(options.target.get), () => ())
  }
  private val display = new TermDisplay(
    new OutputStreamWriter(System.out),
    fallbackMode = options.nonInteractive || TermDisplay.defaultFallbackMode
  )
  private val semanticdb = target.resolve("semanticdb")
  private val symbolRoot = target.resolve("symbol")
  private type Symbol = String
  private val symbols =
    new ConcurrentHashMap[Symbol, AtomicReference[d.Symbol]]()
  private val mappingFunction =
    new JFunction[Symbol, AtomicReference[d.Symbol]] {
      override def apply(t: Symbol): AtomicReference[schema.Symbol] =
        new AtomicReference(d.Symbol(symbol = t))
    }
  private def addDefinition(symbol: Symbol, position: d.Position): Unit = {
    val value = symbols.computeIfAbsent(symbol, mappingFunction)
    value.getAndUpdate(new UnaryOperator[d.Symbol] {
      override def apply(t: schema.Symbol): schema.Symbol =
        t.definition.fold(t.copy(definition = Some(position))) { _ =>
          // Do nothing, conflicting symbol definitions, for example js/jvm
          t
        }
    })
  }
  private def addReference(
      filename: String,
      range: d.Range,
      symbol: Symbol
  ): Unit = {
    val value = symbols.computeIfAbsent(symbol, mappingFunction)
    value.getAndUpdate(new UnaryOperator[d.Symbol] {
      override def apply(t: d.Symbol): d.Symbol = {
        val ranges = t.references.getOrElse(filename, d.Ranges())
        val newRanges = ranges.copy(ranges.ranges :+ range)
        val newReferences = t.references + (filename -> newRanges)
        t.copy(references = newReferences)
      }
    })
  }

  def buildSymbolIndex(): Unit = {
    val task = "Building symbol index"
    display.startTask(task, new File("target"))
    display.taskLength(task, paths.length, 0)
    val counter = new AtomicInteger()
    paths.foreach { path =>
      try {
        display.taskProgress(task, counter.getAndIncrement())
        val bytes = path.readAllBytes
        val db = s.Database.parseFrom(bytes)
        db.documents.foreach { document =>
          document.names.foreach {
            case s.ResolvedName(_, sym, _)
                if !sym.endsWith(".") && !sym.endsWith("#") =>
            // Do nothing, local symbol.
            case s.ResolvedName(Some(s.Position(start, end)), sym, true) =>
              addDefinition(sym, d.Position(document.filename, start, end))
            case s.ResolvedName(Some(s.Position(start, end)), sym, false) =>
              addReference(document.filename, d.Range(start, end), sym)
            case _ =>
          }
          val out = semanticdb.resolve(document.filename)
          Files.createDirectories(out.toNIO.getParent)
          overwrite(
            out.toNIO
              .resolveSibling(out.toNIO.getFileName.toString + ".semanticdb"),
            s.Database(document :: Nil).toByteArray
          )
        }
      } catch {
        case NonFatal(e) =>
          System.err.println(s"$path")
          val shortTrace = e.getStackTrace.take(10)
          e.setStackTrace(shortTrace)
          e.printStackTrace(new PrintStream(System.err))
      }
    }
  }

  private def overwrite(out: Path, bytes: Array[Byte]): Unit = {
    Files.write(
      out,
      bytes,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE
    )
  }

  def writeSymbolIndex(): Unit = {
    import scala.collection.JavaConverters._
    val task = "Writing symbol index"
    display.startTask(task, new File("target"))
    display.taskLength(task, symbols.size(), 0)
    var counter = 0
    symbols.asScala.foreach {
      case (_, ref) =>
        val symbol = ref.get()
        display.taskProgress(task, { counter += 1; counter })
        if (symbol.definition.isDefined) {
          val url = MetadocCli.encodeSymbolName(symbol.symbol)
          val out = symbolRoot.resolve(url)
          Files.createDirectories(out.toNIO.getParent)
          Files.write(
            out.toNIO,
            ref.get.toByteArray,
            StandardOpenOption.CREATE
          )
        }
    }
  }

  def writeFilesnames(): Unit = {
    import scala.collection.JavaConverters._
    val index = d.Index(files = filenames.asScala.toSeq)
    Files.write(
      target.resolve("metadoc.index").toNIO,
      index.toByteArray,
      StandardOpenOption.TRUNCATE_EXISTING,
      StandardOpenOption.CREATE
    )
  }

  def writeAssets(): Unit = {
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

  def run(): Unit = {
    try {
      display.init()
      Files.createDirectories(target.toNIO)
      buildSymbolIndex()
      writeSymbolIndex()
      writeAssets()
    } finally {
      display.stop()
      onClose()
    }
  }
}

object MetadocCli extends CaseApp[MetadocOptions] {

  def encodeSymbolName(name: String): String = {
    val md = java.security.MessageDigest.getInstance("SHA-512")
    val sha = md.digest(name.getBytes("UTF-8"))
    // 512 bits ~> 64 bytes and doubled for the hex encoding
    String.format("%0128x", new java.math.BigInteger(1, sha))
  }

  def semanticdbs(paths: Seq[AbsolutePath]): ArrayBuffer[AbsolutePath] = {
    val files = ArrayBuffer.newBuilder[AbsolutePath]
    val visitor = new SimpleFileVisitor[Path] {
      override def visitFile(
          file: Path,
          attrs: BasicFileAttributes
      ): FileVisitResult = {
        if (file.getFileName.toString.endsWith(".semanticdb")) {
          files += AbsolutePath(file)
        }
        FileVisitResult.CONTINUE
      }
    }
    paths.foreach { path =>
      Files.walkFileTree(path.toNIO, visitor)
    }
    files.result()
  }

  def run(options: MetadocOptions, remainingArgs: RemainingArgs): Unit = {
    val classpath = remainingArgs.remainingArgs.flatMap { cp =>
      cp.split(File.pathSeparator).map(AbsolutePath(_))
    }
    val files = semanticdbs(classpath)
    val runner = new CliRunner(files.par, options)
    runner.run()
    println(options.target.get)
  }
}
