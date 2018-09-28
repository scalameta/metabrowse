package metadoc.tests

import java.nio.file._
import metadoc.{schema => d}
import scala.meta.internal.io.FileIO
import GeneratedSiteEnrichments._

class MetadocCliSuite extends BaseMetadocCliSuite {

  val expectedFiles =
    """paiges/core/src/main/scala/org/typelevel/paiges/Chunk.scala.semanticdb.gz
      |paiges/core/src/main/scala/org/typelevel/paiges/Doc.scala.semanticdb.gz
      |paiges/core/src/main/scala/org/typelevel/paiges/Document.scala.semanticdb.gz
      |paiges/core/src/main/scala/org/typelevel/paiges/package.scala.semanticdb.gz
      |paiges/core/src/test/scala/org/typelevel/paiges/DocumentTests.scala.semanticdb.gz
      |paiges/core/src/test/scala/org/typelevel/paiges/Generators.scala.semanticdb.gz
      |paiges/core/src/test/scala/org/typelevel/paiges/JsonTest.scala.semanticdb.gz
      |paiges/core/src/test/scala/org/typelevel/paiges/PaigesTest.scala.semanticdb.gz
    """.stripMargin
  test("semanticdb.gz") {
    val obtained = FileIO
      .listAllFilesRecursively(out.resolve("semanticdb"))
      .files
      .sortBy(_.toString())
    assertNoDiffOrPrintExpected(obtained.mkString("\n"), expectedFiles)
  }

  test("package.symbolindexes.gz") {
    val expectedSymbols =
      """
        |org/typelevel/paiges/package.symbolindexes.gz
      """.stripMargin
    val obtained = FileIO
      .listAllFilesRecursively(out.resolve("symbol"))
      .files
      .sortBy(_.toString())
    assertNoDiffOrPrintExpected(obtained.mkString("\n"), expectedSymbols)
  }

  test("indexed symbols") {
    val obtained = FileIO
      .listAllFilesRecursively(out.resolve("symbol"))
      .flatMap { path =>
        d.SymbolIndexes.parseFromCompressedPath(path).indexes
      }
      .map { sym =>
        assert(sym.definition.isDefined)
        sym.symbol
      }
      .sorted
      .mkString("\n")
    val expected =
      """|org/typelevel/paiges/Chunk.
         |org/typelevel/paiges/Chunk.best().
         |org/typelevel/paiges/Chunk.indentMax.
         |org/typelevel/paiges/Chunk.indentTable.
         |org/typelevel/paiges/Chunk.lineToStr().
         |org/typelevel/paiges/Chunk.makeIndentStr().
         |org/typelevel/paiges/Doc#
         |org/typelevel/paiges/Doc#`&:`().
         |org/typelevel/paiges/Doc#`&`().
         |org/typelevel/paiges/Doc#`*`().
         |org/typelevel/paiges/Doc#`+:`().
         |org/typelevel/paiges/Doc#`+`().
         |org/typelevel/paiges/Doc#`/:`().
         |org/typelevel/paiges/Doc#`/`().
         |org/typelevel/paiges/Doc#`:&`().
         |org/typelevel/paiges/Doc#`:+`().
         |org/typelevel/paiges/Doc#`:/`().
         |org/typelevel/paiges/Doc#`<init>`().
         |org/typelevel/paiges/Doc#aligned().
         |org/typelevel/paiges/Doc#bracketBy().
         |org/typelevel/paiges/Doc#flatten().
         |org/typelevel/paiges/Doc#flattenBoolean().
         |org/typelevel/paiges/Doc#flattenOption().
         |org/typelevel/paiges/Doc#grouped().
         |org/typelevel/paiges/Doc#hashCode.
         |org/typelevel/paiges/Doc#isEmpty().
         |org/typelevel/paiges/Doc#line().
         |org/typelevel/paiges/Doc#line(+1).
         |org/typelevel/paiges/Doc#lineOrSpace().
         |org/typelevel/paiges/Doc#lineOrSpace(+1).
         |org/typelevel/paiges/Doc#maxWidth().
         |org/typelevel/paiges/Doc#nested().
         |org/typelevel/paiges/Doc#nonEmpty().
         |org/typelevel/paiges/Doc#render().
         |org/typelevel/paiges/Doc#renderGen().
         |org/typelevel/paiges/Doc#renderStream().
         |org/typelevel/paiges/Doc#renderStreamTrim().
         |org/typelevel/paiges/Doc#renderTrim().
         |org/typelevel/paiges/Doc#renderWideStream().
         |org/typelevel/paiges/Doc#repeat().
         |org/typelevel/paiges/Doc#representation().
         |org/typelevel/paiges/Doc#space().
         |org/typelevel/paiges/Doc#space(+1).
         |org/typelevel/paiges/Doc#tightBracketBy().
         |org/typelevel/paiges/Doc#toString().
         |org/typelevel/paiges/Doc#writeTo().
         |org/typelevel/paiges/Doc#writeToGen().
         |org/typelevel/paiges/Doc#writeToTrim().
         |org/typelevel/paiges/Doc.
         |org/typelevel/paiges/Doc.Align#
         |org/typelevel/paiges/Doc.Align#`<init>`().
         |org/typelevel/paiges/Doc.Align#doc.
         |org/typelevel/paiges/Doc.Align.
         |org/typelevel/paiges/Doc.Concat#
         |org/typelevel/paiges/Doc.Concat#`<init>`().
         |org/typelevel/paiges/Doc.Concat#a.
         |org/typelevel/paiges/Doc.Concat#b.
         |org/typelevel/paiges/Doc.Concat.
         |org/typelevel/paiges/Doc.Empty.
         |org/typelevel/paiges/Doc.Line#
         |org/typelevel/paiges/Doc.Line#`<init>`().
         |org/typelevel/paiges/Doc.Line#asFlatDoc().
         |org/typelevel/paiges/Doc.Line#flattenToSpace.
         |org/typelevel/paiges/Doc.Line.
         |org/typelevel/paiges/Doc.Nest#
         |org/typelevel/paiges/Doc.Nest#`<init>`().
         |org/typelevel/paiges/Doc.Nest#doc.
         |org/typelevel/paiges/Doc.Nest#indent.
         |org/typelevel/paiges/Doc.Nest.
         |org/typelevel/paiges/Doc.Text#
         |org/typelevel/paiges/Doc.Text#`<init>`().
         |org/typelevel/paiges/Doc.Text#str.
         |org/typelevel/paiges/Doc.Text.
         |org/typelevel/paiges/Doc.Union#
         |org/typelevel/paiges/Doc.Union#`<init>`().
         |org/typelevel/paiges/Doc.Union#a.
         |org/typelevel/paiges/Doc.Union#b.
         |org/typelevel/paiges/Doc.Union#bDoc.
         |org/typelevel/paiges/Doc.Union.
         |org/typelevel/paiges/Doc.cat().
         |org/typelevel/paiges/Doc.char().
         |org/typelevel/paiges/Doc.charTable.
         |org/typelevel/paiges/Doc.comma.
         |org/typelevel/paiges/Doc.empty.
         |org/typelevel/paiges/Doc.equivAtWidths().
         |org/typelevel/paiges/Doc.fill().
         |org/typelevel/paiges/Doc.foldDocs().
         |org/typelevel/paiges/Doc.intercalate().
         |org/typelevel/paiges/Doc.line.
         |org/typelevel/paiges/Doc.lineBreak.
         |org/typelevel/paiges/Doc.lineOrEmpty.
         |org/typelevel/paiges/Doc.lineOrSpace.
         |org/typelevel/paiges/Doc.maxSpaceTable.
         |org/typelevel/paiges/Doc.orderingAtWidth().
         |org/typelevel/paiges/Doc.paragraph().
         |org/typelevel/paiges/Doc.space.
         |org/typelevel/paiges/Doc.spaceArray.
         |org/typelevel/paiges/Doc.spaces().
         |org/typelevel/paiges/Doc.split().
         |org/typelevel/paiges/Doc.splitWhitespace.
         |org/typelevel/paiges/Doc.spread().
         |org/typelevel/paiges/Doc.stack().
         |org/typelevel/paiges/Doc.str().
         |org/typelevel/paiges/Doc.tabulate().
         |org/typelevel/paiges/Doc.tabulate(+1).
         |org/typelevel/paiges/Doc.text().
         |org/typelevel/paiges/Document#
         |org/typelevel/paiges/Document#contramap().
         |org/typelevel/paiges/Document#document().
         |org/typelevel/paiges/Document.
         |org/typelevel/paiges/Document.FromToString.
         |org/typelevel/paiges/Document.FromToString.document().
         |org/typelevel/paiges/Document.Ops#
         |org/typelevel/paiges/Document.Ops#doc().
         |org/typelevel/paiges/Document.Ops#instance().
         |org/typelevel/paiges/Document.Ops#self().
         |org/typelevel/paiges/Document.ToDocumentOps#
         |org/typelevel/paiges/Document.ToDocumentOps#toDocumentOps().
         |org/typelevel/paiges/Document.apply().
         |org/typelevel/paiges/Document.documentBoolean.
         |org/typelevel/paiges/Document.documentByte.
         |org/typelevel/paiges/Document.documentChar.
         |org/typelevel/paiges/Document.documentDouble.
         |org/typelevel/paiges/Document.documentFloat.
         |org/typelevel/paiges/Document.documentInt.
         |org/typelevel/paiges/Document.documentIterable().
         |org/typelevel/paiges/Document.documentLong.
         |org/typelevel/paiges/Document.documentShort.
         |org/typelevel/paiges/Document.documentString.
         |org/typelevel/paiges/Document.documentUnit.
         |org/typelevel/paiges/Document.instance().
         |org/typelevel/paiges/Document.ops.
         |org/typelevel/paiges/Document.useToString().
         |org/typelevel/paiges/DocumentTest#
         |org/typelevel/paiges/DocumentTest#`<init>`().
         |org/typelevel/paiges/DocumentTest#document().
         |org/typelevel/paiges/DocumentTest#generatorDrivenConfig.
         |org/typelevel/paiges/Generators.
         |org/typelevel/paiges/Generators.arbDoc.
         |org/typelevel/paiges/Generators.asciiString.
         |org/typelevel/paiges/Generators.cogenDoc.
         |org/typelevel/paiges/Generators.combinators.
         |org/typelevel/paiges/Generators.doc0Gen.
         |org/typelevel/paiges/Generators.folds.
         |org/typelevel/paiges/Generators.genDoc.
         |org/typelevel/paiges/Generators.genTree().
         |org/typelevel/paiges/Generators.generalString.
         |org/typelevel/paiges/Generators.maxDepth.
         |org/typelevel/paiges/Generators.unary.
         |org/typelevel/paiges/Json#
         |org/typelevel/paiges/Json#`<init>`().
         |org/typelevel/paiges/Json#toDoc().
         |org/typelevel/paiges/Json.
         |org/typelevel/paiges/Json.JArray#
         |org/typelevel/paiges/Json.JArray#`<init>`().
         |org/typelevel/paiges/Json.JArray#toDoc().
         |org/typelevel/paiges/Json.JArray#toVector.
         |org/typelevel/paiges/Json.JArray.
         |org/typelevel/paiges/Json.JBool#
         |org/typelevel/paiges/Json.JBool#`<init>`().
         |org/typelevel/paiges/Json.JBool#toBoolean.
         |org/typelevel/paiges/Json.JBool#toDoc().
         |org/typelevel/paiges/Json.JNull.
         |org/typelevel/paiges/Json.JNull.toDoc().
         |org/typelevel/paiges/Json.JNumber#
         |org/typelevel/paiges/Json.JNumber#`<init>`().
         |org/typelevel/paiges/Json.JNumber#toDoc().
         |org/typelevel/paiges/Json.JNumber#toDouble.
         |org/typelevel/paiges/Json.JNumber.
         |org/typelevel/paiges/Json.JObject#
         |org/typelevel/paiges/Json.JObject#`<init>`().
         |org/typelevel/paiges/Json.JObject#toDoc().
         |org/typelevel/paiges/Json.JObject#toMap.
         |org/typelevel/paiges/Json.JString#
         |org/typelevel/paiges/Json.JString#`<init>`().
         |org/typelevel/paiges/Json.JString#str.
         |org/typelevel/paiges/Json.JString#toDoc().
         |org/typelevel/paiges/Json.JString.
         |org/typelevel/paiges/Json.escape().
         |org/typelevel/paiges/JsonTest#
         |org/typelevel/paiges/JsonTest#`<init>`().
         |org/typelevel/paiges/PaigesTest#
         |org/typelevel/paiges/PaigesTest#`<init>`().
         |org/typelevel/paiges/PaigesTest#generatorDrivenConfig.
         |org/typelevel/paiges/PaigesTest#slowRenderTrim().
         |org/typelevel/paiges/PaigesTest.
         |org/typelevel/paiges/PaigesTest.EquivSyntax#
         |org/typelevel/paiges/PaigesTest.EquivSyntax#`<init>`().
         |org/typelevel/paiges/PaigesTest.EquivSyntax#`===`().
         |org/typelevel/paiges/PaigesTest.EquivSyntax#lhs.
         |org/typelevel/paiges/PaigesTest.EquivSyntax().
         |org/typelevel/paiges/PaigesTest.docEquiv.
         |org/typelevel/paiges/package.
         |org/typelevel/paiges/package.call().
      """.stripMargin

    assertNoDiffOrPrintExpected(obtained, expected)
  }

  test("index.workspace.gz") {
    val workspacePath = out.resolve("index.workspace.gz")
    assert(Files.exists(workspacePath.toNIO))
    val workspace = d.Workspace.parseFromCompressedPath(workspacePath)
    assert(workspace.filenames.nonEmpty)
    expectedFiles.lines.filter(_.trim.nonEmpty).foreach { file =>
      assert(workspace.filenames.contains(file.stripSuffix(".semanticdb.gz")))
    }
  }

  checkSymbolIndex(
    "org/typelevel/paiges/Json.JArray.",
    """
      |symbol: "org/typelevel/paiges/Json.JArray."
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
      |      startLine: 55
      |      startCharacter: 16
      |      endLine: 55
      |      endCharacter: 22
      |    }
      |    ranges {
      |      startLine: 56
      |      startCharacter: 16
      |      endLine: 56
      |      endCharacter: 22
      |    }
      |  }
      |}
    """.stripMargin
  )

  checkSymbolIndex(
    "org/typelevel/paiges/Json.JArray#",
    """
      |symbol: "org/typelevel/paiges/Json.JArray#"
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
      |      startLine: 55
      |      startCharacter: 16
      |      endLine: 55
      |      endCharacter: 22
      |    }
      |    ranges {
      |      startLine: 56
      |      startCharacter: 16
      |      endLine: 56
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
