package metabrowse.tests

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.meta.internal.semanticdb._
import scala.meta.interactive._
import scala.meta.testkit.DiffAssertions
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
import org.scalatest.FunSuite
import metabrowse.tests.GeneratedSiteEnrichments._

class JsonSuite extends FunSuite with DiffAssertions with BeforeAndAfterAll {
  val compiler: Global = InteractiveSemanticdb.newCompiler()
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
      MetabrowseOptions(target = Some(out.toString)),
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
      """
        |symbol: "com/bar/Main."
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
      """.stripMargin
    )
  }
}
