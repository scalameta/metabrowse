package metadoc.tests

import java.nio.file.Files
import scala.meta.testkit._
import caseapp.RemainingArgs
import metadoc.cli.MetadocCli
import metadoc.cli.MetadocOptions
import metadoc.{schema => d}
import org.langmeta.internal.io.FileIO
import org.langmeta.io.AbsolutePath
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

class MetadocCliTest
    extends FunSuite
    with BeforeAndAfterAll
    with DiffAssertions {
  var out: AbsolutePath = _
  def options = MetadocOptions(
    Some(out.toString()),
    cleanTargetFirst = true,
    nonInteractive = true
  )
  def files = BuildInfo.exampleClassDirectory.map(_.getAbsolutePath)

  def runCli(): Unit = MetadocCli.run(options, RemainingArgs(files, Nil))

  override def beforeAll(): Unit = {
    out = AbsolutePath(Files.createTempDirectory("metadoc"))
    out.toFile.deleteOnExit()
    runCli()
  }

  val expectedFiles =
    """paiges/core/src/main/scala/org/typelevel/paiges/Chunk.scala.semanticdb
      |paiges/core/src/main/scala/org/typelevel/paiges/Doc.scala.semanticdb
      |paiges/core/src/main/scala/org/typelevel/paiges/Document.scala.semanticdb
      |paiges/core/src/main/scala/org/typelevel/paiges/package.scala.semanticdb
      |paiges/core/src/test/scala/org/typelevel/paiges/DocumentTests.scala.semanticdb
      |paiges/core/src/test/scala/org/typelevel/paiges/Generators.scala.semanticdb
      |paiges/core/src/test/scala/org/typelevel/paiges/JsonTest.scala.semanticdb
      |paiges/core/src/test/scala/org/typelevel/paiges/PaigesTest.scala.semanticdb
    """.stripMargin
  test("target/semanticdb") {
    val obtained = FileIO
      .listAllFilesRecursively(out.resolve("semanticdb"))
      .files
      .sortBy(_.toString())
    assertNoDiff(obtained.mkString("\n"), expectedFiles)
  }

  test("target/symbol") {
    val obtained = FileIO
      .listAllFilesRecursively(out.resolve("symbol"))
      .map { path =>
        val sym = d.SymbolIndex.parseFrom(path.readAllBytes)
        assert(sym.definition.isDefined)
        sym.symbol
      }
      .sorted
      .mkString("\n")
    val expected =
      """|
         |org.typelevel.paiges.Chunk.
         |org.typelevel.paiges.Chunk.best(Int,Doc).
         |org.typelevel.paiges.Chunk.indentMax.
         |org.typelevel.paiges.Chunk.indentTable.
         |org.typelevel.paiges.Chunk.lineToStr(Int).
         |org.typelevel.paiges.Chunk.makeIndentStr(Int).
         |org.typelevel.paiges.Doc#
         |org.typelevel.paiges.Doc#`&:`(String).
         |org.typelevel.paiges.Doc#`&`(Doc).
         |org.typelevel.paiges.Doc#`*`(Int).
         |org.typelevel.paiges.Doc#`+:`(String).
         |org.typelevel.paiges.Doc#`+`(Doc).
         |org.typelevel.paiges.Doc#`/:`(String).
         |org.typelevel.paiges.Doc#`/`(Doc).
         |org.typelevel.paiges.Doc#`:&`(String).
         |org.typelevel.paiges.Doc#`:+`(String).
         |org.typelevel.paiges.Doc#`:/`(String).
         |org.typelevel.paiges.Doc#`<init>`().
         |org.typelevel.paiges.Doc#aligned().
         |org.typelevel.paiges.Doc#bracketBy(Doc,Doc,Int).
         |org.typelevel.paiges.Doc#flatten().
         |org.typelevel.paiges.Doc#flattenBoolean().
         |org.typelevel.paiges.Doc#flattenOption().
         |org.typelevel.paiges.Doc#grouped().
         |org.typelevel.paiges.Doc#hashCode().
         |org.typelevel.paiges.Doc#isEmpty().
         |org.typelevel.paiges.Doc#line(Doc).
         |org.typelevel.paiges.Doc#line(String).
         |org.typelevel.paiges.Doc#lineOrSpace(Doc).
         |org.typelevel.paiges.Doc#lineOrSpace(String).
         |org.typelevel.paiges.Doc#maxWidth().
         |org.typelevel.paiges.Doc#nested(Int).
         |org.typelevel.paiges.Doc#nonEmpty().
         |org.typelevel.paiges.Doc#render(Int).
         |org.typelevel.paiges.Doc#renderStream(Int).
         |org.typelevel.paiges.Doc#renderWideStream().
         |org.typelevel.paiges.Doc#repeat(Int).
         |org.typelevel.paiges.Doc#representation(Boolean).
         |org.typelevel.paiges.Doc#space(Doc).
         |org.typelevel.paiges.Doc#space(String).
         |org.typelevel.paiges.Doc#tightBracketBy(Doc,Doc,Int).
         |org.typelevel.paiges.Doc#toString().
         |org.typelevel.paiges.Doc#writeTo(Int,PrintWriter).
         |org.typelevel.paiges.Doc.
         |org.typelevel.paiges.Doc.Align#
         |org.typelevel.paiges.Doc.Align#`<init>`(Doc).
         |org.typelevel.paiges.Doc.Align#doc().
         |org.typelevel.paiges.Doc.Align.
         |org.typelevel.paiges.Doc.Concat#
         |org.typelevel.paiges.Doc.Concat#`<init>`(Doc,Doc).
         |org.typelevel.paiges.Doc.Concat#a().
         |org.typelevel.paiges.Doc.Concat#b().
         |org.typelevel.paiges.Doc.Concat.
         |org.typelevel.paiges.Doc.Empty.
         |org.typelevel.paiges.Doc.Line#
         |org.typelevel.paiges.Doc.Line#`<init>`(Boolean).
         |org.typelevel.paiges.Doc.Line#asFlatDoc().
         |org.typelevel.paiges.Doc.Line#flattenToSpace().
         |org.typelevel.paiges.Doc.Line.
         |org.typelevel.paiges.Doc.Nest#
         |org.typelevel.paiges.Doc.Nest#`<init>`(Int,Doc).
         |org.typelevel.paiges.Doc.Nest#doc().
         |org.typelevel.paiges.Doc.Nest#indent().
         |org.typelevel.paiges.Doc.Nest.
         |org.typelevel.paiges.Doc.Text#
         |org.typelevel.paiges.Doc.Text#`<init>`(String).
         |org.typelevel.paiges.Doc.Text#str().
         |org.typelevel.paiges.Doc.Text.
         |org.typelevel.paiges.Doc.Union#
         |org.typelevel.paiges.Doc.Union#`<init>`(Doc,Function0).
         |org.typelevel.paiges.Doc.Union#a().
         |org.typelevel.paiges.Doc.Union#b().
         |org.typelevel.paiges.Doc.Union#bDoc().
         |org.typelevel.paiges.Doc.Union.
         |org.typelevel.paiges.Doc.cat(Iterable).
         |org.typelevel.paiges.Doc.char(Char).
         |org.typelevel.paiges.Doc.charTable.
         |org.typelevel.paiges.Doc.comma().
         |org.typelevel.paiges.Doc.empty().
         |org.typelevel.paiges.Doc.equivAtWidths(List).
         |org.typelevel.paiges.Doc.fill(Doc,Iterable).
         |org.typelevel.paiges.Doc.foldDocs(Iterable,Function2).
         |org.typelevel.paiges.Doc.intercalate(Doc,Iterable).
         |org.typelevel.paiges.Doc.line().
         |org.typelevel.paiges.Doc.lineBreak().
         |org.typelevel.paiges.Doc.lineOrEmpty().
         |org.typelevel.paiges.Doc.lineOrSpace().
         |org.typelevel.paiges.Doc.maxSpaceTable.
         |org.typelevel.paiges.Doc.orderingAtWidth(Int).
         |org.typelevel.paiges.Doc.paragraph(String).
         |org.typelevel.paiges.Doc.space().
         |org.typelevel.paiges.Doc.spaceArray.
         |org.typelevel.paiges.Doc.spaces(Int).
         |org.typelevel.paiges.Doc.split(String,Regex,Doc).
         |org.typelevel.paiges.Doc.splitWhitespace().
         |org.typelevel.paiges.Doc.spread(Iterable).
         |org.typelevel.paiges.Doc.stack(Iterable).
         |org.typelevel.paiges.Doc.str(T).
         |org.typelevel.paiges.Doc.tabulate(Char,String,Iterable).
         |org.typelevel.paiges.Doc.tabulate(List).
         |org.typelevel.paiges.Doc.text(String).
         |org.typelevel.paiges.Document#
         |org.typelevel.paiges.Document#$init$().
         |org.typelevel.paiges.Document#contramap(Function1).
         |org.typelevel.paiges.Document#document(A).
         |org.typelevel.paiges.Document.
         |org.typelevel.paiges.Document.FromToString.
         |org.typelevel.paiges.Document.FromToString.document(Any).
         |org.typelevel.paiges.Document.apply(Document).
         |org.typelevel.paiges.Document.documentBoolean().
         |org.typelevel.paiges.Document.documentByte().
         |org.typelevel.paiges.Document.documentChar().
         |org.typelevel.paiges.Document.documentDouble().
         |org.typelevel.paiges.Document.documentFloat().
         |org.typelevel.paiges.Document.documentInt().
         |org.typelevel.paiges.Document.documentIterable(String,Document).
         |org.typelevel.paiges.Document.documentLong().
         |org.typelevel.paiges.Document.documentShort().
         |org.typelevel.paiges.Document.documentString().
         |org.typelevel.paiges.Document.documentUnit().
         |org.typelevel.paiges.Document.instance(Function1).
         |org.typelevel.paiges.Document.useToString().
         |org.typelevel.paiges.DocumentTest#
         |org.typelevel.paiges.DocumentTest#`<init>`().
         |org.typelevel.paiges.DocumentTest#document(A,Document).
         |org.typelevel.paiges.DocumentTest#generatorDrivenConfig().
         |org.typelevel.paiges.Generators.
         |org.typelevel.paiges.Generators.arbDoc().
         |org.typelevel.paiges.Generators.asciiString().
         |org.typelevel.paiges.Generators.cogenDoc().
         |org.typelevel.paiges.Generators.combinators().
         |org.typelevel.paiges.Generators.doc0Gen().
         |org.typelevel.paiges.Generators.folds().
         |org.typelevel.paiges.Generators.genDoc().
         |org.typelevel.paiges.Generators.genTree(Int).
         |org.typelevel.paiges.Generators.generalString().
         |org.typelevel.paiges.Generators.maxDepth().
         |org.typelevel.paiges.Generators.unary().
         |org.typelevel.paiges.Json#
         |org.typelevel.paiges.Json#`<init>`().
         |org.typelevel.paiges.Json#toDoc().
         |org.typelevel.paiges.Json.
         |org.typelevel.paiges.Json.JArray#
         |org.typelevel.paiges.Json.JArray#`<init>`(Vector).
         |org.typelevel.paiges.Json.JArray#toDoc().
         |org.typelevel.paiges.Json.JArray#toVector().
         |org.typelevel.paiges.Json.JArray.
         |org.typelevel.paiges.Json.JBool#
         |org.typelevel.paiges.Json.JBool#`<init>`(Boolean).
         |org.typelevel.paiges.Json.JBool#toBoolean().
         |org.typelevel.paiges.Json.JBool#toDoc().
         |org.typelevel.paiges.Json.JNull.
         |org.typelevel.paiges.Json.JNull.toDoc().
         |org.typelevel.paiges.Json.JNumber#
         |org.typelevel.paiges.Json.JNumber#`<init>`(Double).
         |org.typelevel.paiges.Json.JNumber#toDoc().
         |org.typelevel.paiges.Json.JNumber#toDouble().
         |org.typelevel.paiges.Json.JNumber.
         |org.typelevel.paiges.Json.JObject#
         |org.typelevel.paiges.Json.JObject#`<init>`(Map).
         |org.typelevel.paiges.Json.JObject#toDoc().
         |org.typelevel.paiges.Json.JObject#toMap().
         |org.typelevel.paiges.Json.JString#
         |org.typelevel.paiges.Json.JString#`<init>`(String).
         |org.typelevel.paiges.Json.JString#str().
         |org.typelevel.paiges.Json.JString#toDoc().
         |org.typelevel.paiges.Json.JString.
         |org.typelevel.paiges.Json.escape(String).
         |org.typelevel.paiges.JsonTest#
         |org.typelevel.paiges.JsonTest#`<init>`().
         |org.typelevel.paiges.PaigesTest#
         |org.typelevel.paiges.PaigesTest#`<init>`().
         |org.typelevel.paiges.PaigesTest#generatorDrivenConfig().
         |org.typelevel.paiges.PaigesTest.
         |org.typelevel.paiges.PaigesTest.EquivSyntax#
         |org.typelevel.paiges.PaigesTest.EquivSyntax#`<init>`(Doc).
         |org.typelevel.paiges.PaigesTest.EquivSyntax#`===`(Doc).
         |org.typelevel.paiges.PaigesTest.EquivSyntax#lhs.
         |org.typelevel.paiges.PaigesTest.docEquiv().
         |org.typelevel.paiges.PaigesTest.docEquiv.$anon#equiv(Doc,Doc).
         |org.typelevel.paiges.package.
         |org.typelevel.paiges.package.call(A,List).
      """.stripMargin

    assertNoDiff(obtained, expected)
  }

  test("target/index.workspace") {
    val workspacePath = out.resolve("index.workspace")
    assert(Files.exists(workspacePath.toNIO))
    val workspace = d.Workspace.parseFrom(workspacePath.readAllBytes)
    assert(workspace.filenames.nonEmpty)
    expectedFiles.lines.filter(_.trim.nonEmpty).foreach { file =>
      assert(workspace.filenames.contains(file.stripSuffix(".semanticdb")))
    }
  }

  def checkSymbolIndex(id: String, expected: String) = {
    test(id) {
      val index = d.SymbolIndex.parseFrom(
        out
          .resolve("symbol")
          .resolve(MetadocCli.encodeSymbolName(id))
          .readAllBytes
      )
      val obtained = index.toProtoString
      println(obtained)
      println()
      assertNoDiff(obtained, expected)
    }
  }

  checkSymbolIndex(
    "org.typelevel.paiges.Json.JArray.",
    """
        |symbol: "org.typelevel.paiges.Json.JArray."
        |definition {
        |  filename: "paiges/core/src/test/scala/org/typelevel/paiges/JsonTest.scala"
        |  startLine: 34
        |  startCharacter: 13
        |  endLine: 34
        |  endCharacter: 19
        |}
        |references {
        |  key: "paiges/core/src/test/scala/org/typelevel/paiges/JsonTest.scala"
        |  value {
        |    ranges {
        |      startLine: 56
        |      startCharacter: 16
        |      endLine: 56
        |      endCharacter: 22
        |    }
        |    ranges {
        |      startLine: 55
        |      startCharacter: 16
        |      endLine: 55
        |      endCharacter: 22
        |    }
        |  }
        |}
      """.stripMargin
  )

  checkSymbolIndex(
    "org.typelevel.paiges.Json.JArray#",
    """
      |symbol: "org.typelevel.paiges.Json.JArray#"
      |definition {
      |  filename: "paiges/core/src/test/scala/org/typelevel/paiges/JsonTest.scala"
      |  startLine: 34
      |  startCharacter: 13
      |  endLine: 34
      |  endCharacter: 19
      |}
      |references {
      |  key: "paiges/core/src/test/scala/org/typelevel/paiges/JsonTest.scala"
      |  value {
      |    ranges {
      |      startLine: 56
      |      startCharacter: 16
      |      endLine: 56
      |      endCharacter: 22
      |    }
      |    ranges {
      |      startLine: 55
      |      startCharacter: 16
      |      endLine: 55
      |      endCharacter: 22
      |    }
      |  }
      |}
      |""".stripMargin
  )

  test("--clean-target-first") {
    runCli() // assert no error
  }
}
