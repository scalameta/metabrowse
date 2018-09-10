package metadoc.tests

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.meta.Database
import scala.meta.internal.semanticdb._
import scala.meta.interactive._
import scala.meta.testkit.DiffAssertions
import scala.tools.nsc.interactive.Global
import caseapp.RemainingArgs
import scalapb.json4s.JsonFormat
import metadoc.cli.MetadocCli
import metadoc.cli.MetadocOptions
import metadoc.schema.SymbolIndex
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

class JsonSuite extends FunSuite with DiffAssertions with BeforeAndAfterAll {
  val compiler: Global = InteractiveSemanticdb.newCompiler()
  override def afterAll(): Unit = compiler.askShutdown()
  test(".semanticdb.json") {
    val doc = InteractiveSemanticdb.toDocument(
      compiler,
      """package com.bar
        |import scala.concurrent.Future
        |object Main {
        |  val future = Future.successful(1)
        |  Main.future.map(_ + 1)
        |}
      """.stripMargin
    )
    val db = Database(doc :: Nil).toSchema(PathIO.workingDirectory)
    val dbJson = JsonFormat.toJsonString(db)
    val jsonFile = Files.createTempFile("metadoc", ".semanticdb.json")
    val out = Files.createTempDirectory("metadoc")
    Files.write(jsonFile, dbJson.getBytes(StandardCharsets.UTF_8))
    MetadocCli.run(
      MetadocOptions(target = Some(out.toString)),
      RemainingArgs(jsonFile.toString :: Nil, Nil)
    )
    val symbols = FileIO
      .listAllFilesRecursively(AbsolutePath(out).resolve("symbol"))
      .toList
    assert(symbols.length == 2)
    val index = symbols
      .map(path => SymbolIndex.parseFrom(path.readAllBytes))
      .sortBy(_.symbol)
      .map(_.toProtoString)
      .mkString("\n\n")
    assertNoDiff(
      index,
      """
        |symbol: "com.bar.Main."
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
        |symbol: "com.bar.Main.future()."
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
