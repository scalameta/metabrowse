package metabrowse.tests

import java.io.File
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.meta.internal.semanticdb._
import scala.meta.interactive._
import scala.tools.nsc.interactive.Global
import caseapp.RemainingArgs
import scalapb.json4s.JsonFormat
import metabrowse.cli.MetabrowseCli
import metabrowse.cli.MetabrowseOptions
import metabrowse.schema.SymbolIndex
import metabrowse.schema.SymbolIndexes
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import metabrowse.tests.GeneratedSiteEnrichments._

// FIXME re-enable this test suite: https://github.com/scalameta/metabrowse/issues/272
@org.scalatest.Ignore
class JsonSuite extends AnyFunSuite with DiffAssertions with BeforeAndAfterAll {

  // patched version of https://github.com/scalameta/scalameta/blob/3413f84849aa5c5091d7cb2460ff36a8c20a34be/semanticdb/scalac/library/src/main/scala/scala/meta/interactive/InteractiveSemanticdb.scala#L124-L129
  private def thisClasspath: String = this.getClass.getClassLoader match {
    case url: URLClassLoader =>
      url.getURLs.map(_.toURI.getPath).mkString(File.pathSeparator)
    case cl
        if cl.getClass.getName == "jdk.internal.loader.ClassLoaders$AppClassLoader" =>
      // Required with JDK-11
      sys.props.getOrElse("java.class.path", "")
    case els =>
      throw new IllegalStateException(s"Expected URLClassloader, got $els")
  }

  val compiler: Global = InteractiveSemanticdb.newCompiler(thisClasspath, Nil)
  override def afterAll(): Unit = compiler.askShutdown()
  test(".semanticdb.json") {
    val doc = InteractiveSemanticdb.toTextDocument(
      compiler,
      """package com.bar
        |import scala.concurrent.Future
        |object Main {
        |  val future = Future.successful(1)
        |  Main.future.map(_ + 1)
        |}
      """.stripMargin
    )
    val db = TextDocuments(doc :: Nil)
    val dbJson = JsonFormat.toJsonString(db)
    val jsonFile = Files.createTempFile("metabrowse", ".semanticdb.json")
    val out = Files.createTempDirectory("metabrowse")
    Files.write(jsonFile, dbJson.getBytes(StandardCharsets.UTF_8))
    MetabrowseCli.run(
      MetabrowseOptions(
        target = out.toString,
        sourceroot = Some(BuildInfo.sourceroot.toString)
      ),
      RemainingArgs(jsonFile.toString :: Nil, Nil)
    )

    val symbols = FileIO
      .listAllFilesRecursively(AbsolutePath(out).resolve("symbol"))
      .toList
    assert(symbols.length == 1)
    val index = symbols
      .flatMap(path => SymbolIndexes.parseFromCompressedPath(path).indexes)
      .sortBy(_.symbol)
      .map(_.toProtoString)
      .mkString("\n\n")
    assertNoDiff(
      index,
      """symbol: "com/bar/Main."
        |definition {
        |  filename: "interactive.scala"
        |  startLine: 2
        |  startCharacter: 7
        |  endLine: 2
        |  endCharacter: 11
        |}
        |references {
        |  key: "interactive.scala"
        |  value {
        |    ranges {
        |      startLine: 4
        |      startCharacter: 2
        |      endLine: 4
        |      endCharacter: 6
        |    }
        |  }
        |}
        |
        |
        |symbol: "com/bar/Main.future."
        |definition {
        |  filename: "interactive.scala"
        |  startLine: 3
        |  startCharacter: 6
        |  endLine: 3
        |  endCharacter: 12
        |}
        |references {
        |  key: "interactive.scala"
        |  value {
        |    ranges {
        |      startLine: 4
        |      startCharacter: 7
        |      endLine: 4
        |      endCharacter: 13
        |    }
        |  }
        |}
        |""".stripMargin
    )
  }
}
