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
import org.langmeta.internal.io.FileIO
import org.langmeta.internal.io.PathIO
import org.langmeta.io.AbsolutePath
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
      .mkString("\n\n")
    assertNoDiff(
      index,
      """
        |symbol: "_root_.com.bar.Main."
        |definition {
        |  filename: "interactive.scala"
        |  start: 54
        |  end: 58
        |}
        |references {
        |  key: "interactive.scala"
        |  value {
        |    ranges {
        |      start: 99
        |      end: 103
        |    }
        |  }
        |}
        |
        |
        |symbol: "_root_.com.bar.Main.future."
        |definition {
        |  filename: "interactive.scala"
        |  start: 67
        |  end: 73
        |}
        |references {
        |  key: "interactive.scala"
        |  value {
        |    ranges {
        |      start: 104
        |      end: 110
        |    }
        |  }
        |}
        |
      """.stripMargin
    )
  }
}
