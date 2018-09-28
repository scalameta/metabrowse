package mbrowse.cli

import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.util.zip.ZipInputStream
import scala.collection.{GenSeq, concurrent}
import java.nio.file.attribute.BasicFileAttributes
import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import java.util.function.{Function => JFunction}
import scala.collection.parallel.mutable.ParArray
import scala.util.control.NonFatal
import caseapp._
import caseapp.core.Messages
import java.util.zip.GZIPOutputStream
import scalapb.json4s.JsonFormat
import mbrowse.schema
import mbrowse.schema.SymbolIndex
import mbrowse.{schema => d}
import scala.meta._
import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.semanticdb.Scala._
import mbrowse.MbrowseEnrichments._
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.internal.mbrowse.ScalametaInternals
import scala.collection.JavaConverters._
import scala.meta.internal.semanticdb.Scala._

@AppName("mbrowse")
@AppVersion("<version>")
@ProgName("mbrowse")
case class MbrowseOptions(
    @HelpMessage(
      "The output directory to generate the mbrowse site. (required)"
    )
    target: Option[String] = None,
    @HelpMessage(
      "The SemanticDB sourceroot used to compiled sources, must match the compiler option -P:semanticdb:sourceroot:<value>. " +
        "Defaults to the working directory if empty."
    )
    sourceroot: Option[String] = None,
    @HelpMessage(
      "Clean the target directory before generating new site. " +
        "All files will be deleted so be careful."
    )
    cleanTargetFirst: Boolean = false,
    @HelpMessage(
      "Experimental. Emit mbrowse.zip file instead of static files."
    )
    zip: Boolean = false,
    @HelpMessage("Disable fancy progress bar")
    nonInteractive: Boolean = false,
    @HelpMessage(
      "The working directory used to relativize file directories, default to sys.props('user.dir') if empty."
    )
    cwd: Option[String] = None,
) {
  def targetPath: AbsolutePath = AbsolutePath(target.get)
}

case class Target(target: AbsolutePath, onClose: () => Unit)

class CliRunner(classpath: Seq[AbsolutePath], options: MbrowseOptions) {
  private implicit val cwd: AbsolutePath =
    options.cwd.fold(PathIO.workingDirectory)(AbsolutePath(_))
  private val sourceroot: AbsolutePath =
    options.sourceroot.fold(PathIO.workingDirectory)(AbsolutePath(_))
  val Target(target, onClose) = if (options.zip) {
    // For large corpora (>1M LOC) writing the symbol/ directory is the
    // bottle-neck unless --zip is enabled.
    val out = options.targetPath.resolve("mbrowse.zip")
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
    Target(options.targetPath, () => ())
  }
  private val display = new TermDisplay(
    new OutputStreamWriter(System.out),
    fallbackMode = options.nonInteractive || TermDisplay.defaultFallbackMode
  )
  private val semanticdb = target.resolve("semanticdb")
  private val symbolRoot = target.resolve("symbol")
  private val filenames = new ConcurrentSkipListSet[String]()
  private val symbols =
    new ConcurrentHashMap[String, AtomicReference[d.SymbolIndex]]()
  private val mappingFunction: JFunction[
    String,
    AtomicReference[d.SymbolIndex]
  ] = { t =>
    new AtomicReference(d.SymbolIndex(symbol = t))
  }

  private def overwrite(out: Path, bytes: Array[Byte]): Unit = {
    val gzout = out.resolveSibling(out.getFileName.toString + ".gz")
    val fos = Files.newOutputStream(gzout)
    try {
      val gos = new GZIPOutputStream(fos, bytes.length)
      try {
        gos.write(bytes)
        gos.finish()
      } finally {
        gos.close()
      }
    } finally {
      fos.close()
    }
  }

  private def addDefinition(symbol: String, position: d.Position): Unit = {
    val value = symbols.computeIfAbsent(symbol, mappingFunction)
    value.getAndUpdate(new UnaryOperator[d.SymbolIndex] {
      override def apply(t: schema.SymbolIndex): schema.SymbolIndex =
        t.definition.fold(t.copy(definition = Some(position))) { _ =>
          // Do nothing, conflicting symbol definitions, for example js/jvm
          t
        }
    })
  }

  private def addReference(
      filename: String,
      range: d.Range,
      symbol: String
  ): Unit = {
    val value = symbols.computeIfAbsent(symbol, mappingFunction)
    value.getAndUpdate(new UnaryOperator[d.SymbolIndex] {
      override def apply(t: d.SymbolIndex): d.SymbolIndex = {
        val ranges = t.references.getOrElse(filename, d.Ranges())
        val newRanges = ranges.copy(ranges.ranges :+ range)
        val newReferences = t.references + (filename -> newRanges)
        t.copy(references = newReferences)
      }
    })
  }

  type Tick = () => Unit

  private def phase[T](task: String, length: Int)(f: Tick => T): T = {
    display.startTask(task, new File("target"))
    display.taskLength(task, length, 0)
    val counter = new AtomicInteger()
    val tick: Tick = { () =>
      display.taskProgress(task, counter.incrementAndGet())
    }
    val result = f(tick)
    display.completedTask(task, success = true)
    result
  }

  def scanSemanticdbs(): GenSeq[AbsolutePath] =
    phase("Scanning semanticdb files", classpath.length) { tick =>
      val files = ParArray.newBuilder[AbsolutePath]
      val visitor = new SimpleFileVisitor[Path] {
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          val filename = file.getFileName.toString
          if (filename.endsWith(".semanticdb") ||
            filename.endsWith(".semanticdb.json")) {
            files += AbsolutePath(file)
          }
          FileVisitResult.CONTINUE
        }
      }
      classpath.foreach { path =>
        tick()
        Files.walkFileTree(path.toNIO, visitor)
      }
      files.result()
    }

  private def updateText(doc: s.TextDocument): s.TextDocument = {
    if (doc.text.isEmpty) {
      val abspath = sourceroot.resolve(doc.uri)
      if (abspath.isFile) {
        val text = FileIO.slurp(abspath, StandardCharsets.UTF_8)
        val md5 = FingerprintOps.md5(text)
        if (md5 != doc.md5) {
          System.err.println(
            s"error: md5 fingerprint mismatch for document ${doc.uri}"
          )
        }
        doc.withText(text)
      } else {
        System.err.println(
          s"error: No file on disc for document ${doc.uri}"
        )
        doc
      }
    } else {
      doc
    }
  }

  private def parseDatabase(path: AbsolutePath): s.TextDocuments = {
    val filename = path.toNIO.getFileName.toString
    val bytes = path.readAllBytes
    if (filename.endsWith(".semanticdb")) {
      s.TextDocuments.parseFrom(bytes)
    } else if (filename.endsWith(".semanticdb.json")) {
      val string = new String(bytes, StandardCharsets.UTF_8)
      JsonFormat.fromJsonString[s.TextDocuments](string)
    } else {
      throw new IllegalArgumentException(s"Unexpected filename $filename")
    }
  }

  def buildSymbolIndex(paths: GenSeq[AbsolutePath]): Unit =
    phase("Building symbol index", paths.length) { tick =>
      paths.foreach { path =>
        try {
          tick()
          val db = parseDatabase(path)
          db.documents.foreach { noTextDocument =>
            val document = updateText(noTextDocument)
            document.occurrences.foreach {
              case s.SymbolOccurrence(_, sym, _)
                  if !sym.endsWith(".") && !sym.endsWith("#") =>
              // Do nothing, local symbol.
              case s.SymbolOccurrence(
                  Some(r),
                  sym,
                  s.SymbolOccurrence.Role.DEFINITION
                  ) =>
                addDefinition(sym, r.toPosition(document.uri))
              case s.SymbolOccurrence(
                  Some(r),
                  sym,
                  s.SymbolOccurrence.Role.REFERENCE
                  ) =>
                addReference(document.uri, r.toDocRange, sym)
              case _ =>
            }
            val out = semanticdb.resolve(document.uri)
            Files.createDirectories(out.toNIO.getParent)
            overwrite(
              out.toNIO
                .resolveSibling(
                  out.toNIO.getFileName.toString + ".semanticdb"
                ),
              s.TextDocuments(document :: Nil).toByteArray
            )
            filenames.add(document.uri)
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

  def symbolIndexByTopLevelSymbol: util.Map[String, List[d.SymbolIndex]] = {
    val byToplevel = new util.HashMap[String, List[d.SymbolIndex]]()
    symbols.asScala.foreach {
      case (sym, ref) =>
        val toplevel = sym.toplevelPackage
        val old = byToplevel.getOrDefault(toplevel, Nil)
        byToplevel.put(toplevel, ref.get() :: old)
    }
    byToplevel
  }

  def writeSymbolIndex(): Unit =
    phase("Writing symbol index", symbols.size()) { tick =>
      Files.createDirectory(symbolRoot.toNIO)
      val symbolsMap = symbols.asScala
      val byToplevel = symbolIndexByTopLevelSymbol.asScala
      byToplevel.foreach {
        case (sym, indexes) =>
          tick()
          val actualIndexes = indexes.map { symbolIndex =>
            val actualIndex = symbolIndex.definition match {
              case Some(_) => updateReferencesForType(symbolsMap, symbolIndex)
              case None => updateDefinitionsForTerm(symbolsMap, symbolIndex)
            }
            actualIndex
          }
          val symbolIndexes =
            d.SymbolIndexes(actualIndexes.filter(_.definition.isDefined))
          if (symbolIndexes.indexes.nonEmpty) {
            val filename = sym.symbolIndexPath.stripSuffix(".gz")
            val out = symbolRoot.resolve(filename)
            Files.createDirectories(out.toNIO.getParent)
            overwrite(out.toNIO, symbolIndexes.toByteArray)
          }
      }
    }

  private def updateReferencesForType(
      symbolsMap: concurrent.Map[String, AtomicReference[SymbolIndex]],
      symbolIndex: SymbolIndex
  ): SymbolIndex = {
    if (symbolIndex.symbol.isType) {
      try {
        val (owner, desc) = ScalametaInternals.ownerAndDesc(symbolIndex.symbol)
        val maybeTermInfo = for {
          syntheticObjRef <- symbolsMap.get(
            Symbols.Global(owner, Descriptor.Term(desc.name.value))
          )
          if syntheticObjRef.get().definition.isEmpty
        } yield symbolIndex.copy(references = syntheticObjRef.get().references)
        maybeTermInfo.getOrElse(symbolIndex)
      } catch {
        case NonFatal(e) =>
          symbolIndex
      }
    } else {
      symbolIndex
    }
  }

  private def updateDefinitionsForTerm(
      symbolsMap: concurrent.Map[String, AtomicReference[SymbolIndex]],
      symbolIndex: SymbolIndex
  ): SymbolIndex = {
    if (symbolIndex.symbol.isTerm) {
      val (owner, desc) = ScalametaInternals.ownerAndDesc(symbolIndex.symbol)
      val maybeTypeInfo = for {
        typeRef <- symbolsMap.get(
          Symbols.Global(owner, Descriptor.Type(desc.name.value))
        )
        definition <- typeRef.get().definition
      } yield symbolIndex.copy(definition = Some(definition))
      maybeTypeInfo.getOrElse(symbolIndex)
    } else {
      symbolIndex
    }
  }

  def writeAssets(): Unit = {
    val root = target.toNIO
    val inputStream = MbrowseCli.getClass.getClassLoader
      .getResourceAsStream("mbrowse-assets.zip")
    if (inputStream == null)
      sys.error("Failed to locate mbrowse-assets.zip on the classpath")
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

        def copyLoop(): Unit = {
          val read = zipStream.read(bytes, 0, bytes.length)
          if (read > 0) {
            out.write(bytes, 0, read)
            copyLoop()
          }
        }

        copyLoop()
        out.flush()
        out.close()
      }
  }

  def writeWorkspace(): Unit = {
    import scala.collection.JavaConverters._
    val workspace = d.Workspace(filenames.asScala.toSeq)
    overwrite(target.resolve("index.workspace").toNIO, workspace.toByteArray)
  }

  def run(): Unit = {
    try {
      display.init()
      Files.createDirectories(target.toNIO)
      val paths = scanSemanticdbs()
      buildSymbolIndex(paths)
      writeSymbolIndex()
      writeAssets()
      writeWorkspace()
    } finally {
      display.stop()
      onClose()
    }
  }
}

object MbrowseCli extends CaseApp[MbrowseOptions] {

  override val messages: Messages[MbrowseOptions] =
    Messages[MbrowseOptions].copy(optionsDesc = "[options] classpath")

  def run(options: MbrowseOptions, remainingArgs: RemainingArgs): Unit = {

    if (options.target.isEmpty) {
      error("--target is required")
    }

    if (options.cleanTargetFirst) {
      import better.files._
      val file = options.targetPath.toFile.toScala
      if (file.exists) file.delete()
    }

    val classpath = remainingArgs.remainingArgs.flatMap { cp =>
      cp.split(File.pathSeparator).map(AbsolutePath(_))
    }
    val runner = new CliRunner(classpath, options)
    runner.run()
    println(options.target.get)
  }
}
