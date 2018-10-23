package metabrowse.server

import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Headers
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLClassLoader
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Scanner
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPOutputStream
import metabrowse.schema.SymbolIndex
import metabrowse.schema.SymbolIndexes
import metabrowse.schema.Workspace
import metabrowse.{schema => d}
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import scala.meta.inputs.Input
import scala.meta.interactive.InteractiveSemanticdb
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.InputStreamIO
import scala.meta.internal.mtags.Enrichments._
import scala.meta.internal.mtags.Mtags
import scala.meta.internal.mtags.OnDemandSymbolIndex
import scala.meta.internal.semanticdb.TextDocuments
import scala.meta.internal.semanticdb.scalac.SemanticdbOps
import scala.meta.internal.{mtags => t}
import scala.meta.io.AbsolutePath
import scala.tools.nsc.interactive.Global
import scala.util.control.NonFatal

class MetabrowseServer(
    scalacOptions: List[String] = Nil,
    host: String = "localhost",
    port: Int = 4000,
    logger: Logger = LoggerFactory.getLogger("MetabrowseServer")
) {

  /** Starts a server that servers sources from the sourcepath. */
  def start(sourcepath: Sourcepath): Unit = {
    replaceClasspath(sourcepath)
    server.start()
  }

  /** Stops the server and cleans up internal state */
  def stop(): Unit = {
    server.stop()
    global.askShutdown()
  }

  /** Updates the running server to use a new sourcepath.
    *
    * Browser clients need to refresh their browser to pick up the new state.
    *
    * Beware: this method is untested!
    */
  def replaceClasspath(sourcepath: Sourcepath): Unit = {
    lock.synchronized {
      if (state.get() != null) {
        global.askShutdown()
      }
      val newState = State(
        OnDemandSymbolIndex(),
        new URLClassLoader(sourcepath.sources.map(_.toUri.toURL).toArray),
        InteractiveSemanticdb.newCompiler(
          sourcepath.classpath.mkString(File.pathSeparator),
          scalacOptions
        ),
        sourcepath
      )
      state.set(newState)
      sourcepath.sources.foreach(jar => index.addSourceJar(AbsolutePath(jar)))
    }
  }

  /** Returns the URL path pointing to the definition location of the given symbol */
  def urlForSymbol(
      compiler: Global
  )(symbol: compiler.Symbol): Option[String] = {
    lazy val semanticdbOps: SemanticdbOps {
      val global: compiler.type
    } = new SemanticdbOps {
      val global: compiler.type = compiler
    }
    import semanticdbOps._
    var compilerSymbol: compiler.Symbol = compiler.rootMirror.RootPackage
    symbol.ownerChain.reverse.drop(1).foreach { owner =>
      val name =
        if (owner.name.isTermName || owner.hasPackageFlag) {
          compiler.TermName(owner.nameString)
        } else {
          compiler.TypeName(owner.nameString)
        }
      compilerSymbol = compilerSymbol.info.member(name)
    }
    val semanticdbSymbol = compilerSymbol.toSemantic
    for {
      symbolIndex <- getSymbol(semanticdbSymbol).indexes.headOption
      position <- symbolIndex.definition
    } yield {
      s"#${position.filename}#L${position.startLine}C${position.startCharacter}"
    }
  }

  // Mutable state:
  case class State(
      index: OnDemandSymbolIndex,
      classLoader: URLClassLoader,
      global: Global,
      sourcepath: Sourcepath
  )
  private val state = new AtomicReference[State]()
  def index = state.get().index
  def classLoader = state.get().classLoader
  def global = state.get().global
  def sourcepath = state.get().sourcepath

  // Static state:
  private val lock = new Object()
  private val assets = {
    val in =
      this.getClass.getClassLoader.getResourceAsStream("metabrowse-assets.zip")
    val out = Files.createTempDirectory("metabrowse").resolve("assets.zip")
    Files.copy(in, out)
    FileIO.jarRootPath(AbsolutePath(out))
  }
  private val httpHandler = new HttpHandler {
    override def handleRequest(exchange: HttpServerExchange): Unit = {
      val bytes =
        try {
          lock.synchronized {
            getBytes(exchange)
          }
        } catch {
          case NonFatal(e) =>
            logger.error(s"unexpected error: $exchange", e)
            Array.emptyByteArray
        }
      val compressed = gzipDeflate(bytes)
      val buffer = ByteBuffer.wrap(compressed)
      exchange.getResponseHeaders.put(
        Headers.CONTENT_ENCODING,
        "gzip"
      )
      exchange.getResponseHeaders.put(
        Headers.CONTENT_TYPE,
        contentType(exchange.getRequestPath)
      )
      exchange.getResponseSender.send(buffer)
    }
  }
  private val server = Undertow
    .builder()
    .addHttpListener(port, host)
    .setHandler(httpHandler)
    .build()

  private def getBytes(exchange: HttpServerExchange): Array[Byte] = {
    val path = exchange.getRequestPath.stripSuffix(".gz")
    if (path.endsWith("index.workspace")) {
      getWorkspace.toByteArray
    } else if (path.endsWith(".symbolindexes")) {
      val header = exchange.getRequestHeaders.get("Metabrowse-Symbol")
      if (header.isEmpty) {
        logger.error(s"no Metabrowse-Symbol header: $exchange")
        Array.emptyByteArray
      } else {
        getSymbol(header.getFirst).toByteArray
      }
    } else if (path.endsWith(".semanticdb")) {
      getSemanticdb(path).toByteArray
    } else if (path.endsWith(".map")) {
      // Ignore requests for sourcemaps.
      Array.emptyByteArray
    } else {
      val actualPath = if (path == "/") "/index.html" else path
      val file = assets.resolve(actualPath)
      if (file.isFile) file.readAllBytes
      else {
        logger.warn(s"no such file: $file")
        Array.emptyByteArray
      }
    }
  }

  private def gzipDeflate(bytes: Array[Byte]): Array[Byte] = {
    if (bytes.isEmpty) bytes
    else {
      val baos = new ByteArrayOutputStream()
      val gos = new GZIPOutputStream(baos, bytes.length)
      try {
        gos.write(bytes)
        gos.finish()
        baos.toByteArray
      } finally {
        gos.close()
      }
    }
  }

  private def getWorkspace: Workspace = {
    val filenames = ArrayBuffer.newBuilder[String]
    for {
      sourcesJar <- sourcepath.sources
    } {
      FileIO.withJarFileSystem(
        AbsolutePath(sourcesJar),
        create = false,
        close = false
      ) { root =>
        FileIO
          .listAllFilesRecursively(root)
          .filter(!_.toLanguage.isUnknownLanguage)
          .foreach(path => filenames += path.toNIO.toString.stripPrefix("/"))
      }
    }
    Workspace(filenames.result())
  }

  private def getSemanticdb(filename: String): TextDocuments = {
    val path = filename
      .stripPrefix("/semanticdb/")
      .stripPrefix("/") // optional '/'
      .stripSuffix(".semanticdb")
    logger.info(path)
    for {
      in <- Option(classLoader.getResourceAsStream(path)).orElse {
        logger.warn(s"no source file: $path")
        None
      }
      text = new String(InputStreamIO.readBytes(in), StandardCharsets.UTF_8)
      doc <- try {
        val timeout = TimeUnit.SECONDS.toMillis(10)
        val textDocument = if (path.endsWith(".java")) {
          val input = Input.VirtualFile(path, text)
          Mtags.index(input)
        } else {
          InteractiveSemanticdb.toTextDocument(
            global,
            text,
            filename,
            timeout,
            List("-P:semanticdb:symbols:none")
          )
        }
        Some(textDocument)
      } catch {
        case NonFatal(e) =>
          logger.error(s"compile error: $filename", e)
          None
      }
    } yield TextDocuments(List(doc.withText(text)))
  }.getOrElse(TextDocuments())

  private def getSymbol(sym: String): SymbolIndexes = {
    val definition =
      for {
        defn <- index
          .definition(t.Symbol(sym))
          .orElse {
            logger.error(s"no definition for symbol: '$sym'")
            None
          }
        input = defn.path.toInput
        doc = Mtags.index(input)
        occ <- doc.occurrences
          .find { occ =>
            occ.role.isDefinition &&
            occ.range.isDefined &&
            occ.symbol == defn.definitionSymbol.value
          }
          .orElse {
            logger.error(s"no definition occurrence: $defn")
            None
          }
        range <- occ.range.orElse {
          logger.error(s"no range: $occ")
          None
        }
      } yield {
        d.Position(
          defn.path.toString(),
          startLine = range.startLine,
          startCharacter = range.startCharacter,
          endLine = range.endLine,
          endCharacter = range.endCharacter
        )
      }
    SymbolIndexes(
      List(
        SymbolIndex(
          symbol = sym,
          definition = definition
        )
      )
    )
  }

  private def contentType(path: String): String = {
    if (path.endsWith(".js")) "application/javascript"
    else if (path.endsWith(".css")) "text/css"
    else if (path.endsWith(".html")) "text/html"
    else ""
  }
}

object MetabrowseServer {

  /**
    * Basic command-line interface to start metabrowse-server.
    *
    * Examples: {{{
    *
    *   // browse multiple artifacts with no custom compiler flags
    *   metabrowse-server org.scalameta:scalameta_2.12:4.0.0 org.typelevel:paiges_2.12:0.2.1
    *
    *   // browse artifact with custom compiler flags
    *   metabrowse-server -Yrangepos -Xfatal-warning -- org.scalameta:scalameta_2.12:4.0.0
    *
    *   // browse artifact with macroparadise and kind-project plugins enabled
    *   metabrowse-server macroparadise -- org.scalameta:scalameta_2.12:4.0.0
    *
    * }}}
    *
    * This basic interface exists primarily for local testing purposes, it's probably best
    * to use a proper command-line parsing library down the road,
    */
  def main(arrayArgs: Array[String]): Unit = {
    val args = arrayArgs.iterator.map {
      case "macroparadise" => s"-Xplugin:$macroParadise"
      case "kind-projector" => s"-Xplugin:$kindProjector"
      case flag => flag
    }.toList
    val dash = args.indexOf("--")
    val (scalacOptions, artifacts) = {
      if (dash < 0) {
        (Nil, args)
      } else {
        (args.slice(0, dash), args.slice(dash + 1, args.length))
      }
    }
    val sourcepath = Sourcepath(artifacts)
    val server = new MetabrowseServer(scalacOptions = scalacOptions)
    val in = new Scanner(System.in)
    server.start(sourcepath)
    try {
      println(
        "Listening to http://localhost:4000/ (press enter to stop server)"
      )
      in.nextLine()
    } catch {
      case NonFatal(_) =>
    } finally {
      println("Stopping server...")
      server.stop()
    }
  }

  private def macroParadise: Path =
    Sourcepath.coursierFetchCompilerPlugin(
      s"org.scalamacros:paradise_${scalaFullVersion}:2.1.0"
    )
  private def kindProjector: Path =
    Sourcepath.coursierFetchCompilerPlugin(
      s"org.spire-math:kind-projector_${scalaBinaryVersion}:0.9.8"
    )
  private def scalaFullVersion: String =
    scala.util.Properties.versionNumberString
  private def scalaBinaryVersion: String =
    scala.util.Properties.versionNumberString.split("\\.").take(2).mkString(".")
}
