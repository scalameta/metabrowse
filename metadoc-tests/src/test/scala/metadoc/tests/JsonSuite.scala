package metadoc.tests

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.meta.Database
import scala.meta.internal.semanticdb._
import scala.meta.interactive._
import scala.meta.testkit.DiffAssertions
import scala.tools.nsc.interactive.Global
import caseapp.RemainingArgs
import metadoc.cli.MetadocCli
import metadoc.cli.MetadocOptions
import metadoc.schema.SymbolIndex
import org.langmeta.internal.io.FileIO
import org.langmeta.internal.io.PathIO
import org.langmeta.io.AbsolutePath
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import scalapb.json4s.JsonFormat

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
        |SymbolIndex(_root_.com.bar.Main.,Some(Position(interactive.scala,2,7,2,11)),Map(interactive.scala -> Ranges(Vector(Range(4,2,4,6)))))
        |
        |SymbolIndex(_root_.com.bar.Main.future.,Some(Position(interactive.scala,3,6,3,12)),Map(interactive.scala -> Ranges(Vector(Range(4,7,4,13)))))
        |
      """.stripMargin
    )
  }
}
