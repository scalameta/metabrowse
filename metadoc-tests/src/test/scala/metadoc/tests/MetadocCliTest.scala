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
  override def beforeAll(): Unit = {
    out = AbsolutePath(Files.createTempDirectory("metadoc"))
    out.toFile.deleteOnExit()

    val options = MetadocOptions(
      Some(out.toString()),
      cleanTargetFirst = true,
      nonInteractive = true
    )
    val files = BuildInfo.exampleClassDirectory.map(_.getAbsolutePath)
    MetadocCli.run(options, RemainingArgs(files, Nil))
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
        val sym = d.Symbol.parseFrom(path.readAllBytes)
        assert(sym.definition.isDefined)
        sym.symbol
      }
      .sorted
      .mkString("\n")
    val expected =
      """|
         |_root_.org.typelevel.paiges.Chunk.
         |_root_.org.typelevel.paiges.Chunk.best(ILorg/typelevel/paiges/Doc;)Lscala/collection/Iterator;.
         |_root_.org.typelevel.paiges.Chunk.indentMax.
         |_root_.org.typelevel.paiges.Chunk.indentTable.
         |_root_.org.typelevel.paiges.Chunk.lineToStr(I)Ljava/lang/String;.
         |_root_.org.typelevel.paiges.Chunk.makeIndentStr(I)Ljava/lang/String;.
         |_root_.org.typelevel.paiges.Doc#
         |_root_.org.typelevel.paiges.Doc#`&:`(Ljava/lang/String;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#`&`(Lorg/typelevel/paiges/Doc;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#`*`(I)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#`+:`(Ljava/lang/String;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#`+`(Lorg/typelevel/paiges/Doc;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#`/:`(Ljava/lang/String;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#`/`(Lorg/typelevel/paiges/Doc;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#`:&`(Ljava/lang/String;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#`:+`(Ljava/lang/String;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#`:/`(Ljava/lang/String;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#`<init>`()V.
         |_root_.org.typelevel.paiges.Doc#aligned()Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#bracketBy(Lorg/typelevel/paiges/Doc;Lorg/typelevel/paiges/Doc;I)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#flatten()Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#flattenBoolean()Lscala/Tuple2;.
         |_root_.org.typelevel.paiges.Doc#flattenOption()Lscala/Option;.
         |_root_.org.typelevel.paiges.Doc#grouped()Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#hashCode.
         |_root_.org.typelevel.paiges.Doc#isEmpty()Z.
         |_root_.org.typelevel.paiges.Doc#line(Ljava/lang/String;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#line(Lorg/typelevel/paiges/Doc;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#lineOrSpace(Ljava/lang/String;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#lineOrSpace(Lorg/typelevel/paiges/Doc;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#maxWidth()I.
         |_root_.org.typelevel.paiges.Doc#nested(I)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#nonEmpty()Z.
         |_root_.org.typelevel.paiges.Doc#render(I)Ljava/lang/String;.
         |_root_.org.typelevel.paiges.Doc#renderStream(I)Lscala/collection/immutable/Stream;.
         |_root_.org.typelevel.paiges.Doc#renderWideStream()Lscala/collection/immutable/Stream;.
         |_root_.org.typelevel.paiges.Doc#repeat(I)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#representation(Z)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#space(Ljava/lang/String;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#space(Lorg/typelevel/paiges/Doc;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#tightBracketBy(Lorg/typelevel/paiges/Doc;Lorg/typelevel/paiges/Doc;I)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc#toString()Ljava/lang/String;.
         |_root_.org.typelevel.paiges.Doc#writeTo(ILjava/io/PrintWriter;)V.
         |_root_.org.typelevel.paiges.Doc.
         |_root_.org.typelevel.paiges.Doc.Align#
         |_root_.org.typelevel.paiges.Doc.Align#`<init>`(Lorg/typelevel/paiges/Doc;)V.
         |_root_.org.typelevel.paiges.Doc.Concat#
         |_root_.org.typelevel.paiges.Doc.Concat#`<init>`(Lorg/typelevel/paiges/Doc;Lorg/typelevel/paiges/Doc;)V.
         |_root_.org.typelevel.paiges.Doc.Empty.
         |_root_.org.typelevel.paiges.Doc.Line#
         |_root_.org.typelevel.paiges.Doc.Line#`<init>`(Z)V.
         |_root_.org.typelevel.paiges.Doc.Line#asFlatDoc()Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.Nest#
         |_root_.org.typelevel.paiges.Doc.Nest#`<init>`(ILorg/typelevel/paiges/Doc;)V.
         |_root_.org.typelevel.paiges.Doc.Text#
         |_root_.org.typelevel.paiges.Doc.Text#`<init>`(Ljava/lang/String;)V.
         |_root_.org.typelevel.paiges.Doc.Union#
         |_root_.org.typelevel.paiges.Doc.Union#`<init>`(Lorg/typelevel/paiges/Doc;Lscala/Function0;)V.
         |_root_.org.typelevel.paiges.Doc.Union#bDoc.
         |_root_.org.typelevel.paiges.Doc.cat(Lscala/collection/Iterable;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.char(C)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.charTable.
         |_root_.org.typelevel.paiges.Doc.comma.
         |_root_.org.typelevel.paiges.Doc.empty.
         |_root_.org.typelevel.paiges.Doc.equivAtWidths(Lscala/collection/immutable/List;)Lscala/math/Equiv;.
         |_root_.org.typelevel.paiges.Doc.fill(Lorg/typelevel/paiges/Doc;Lscala/collection/Iterable;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.foldDocs(Lscala/collection/Iterable;Lscala/Function2;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.intercalate(Lorg/typelevel/paiges/Doc;Lscala/collection/Iterable;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.line.
         |_root_.org.typelevel.paiges.Doc.lineBreak.
         |_root_.org.typelevel.paiges.Doc.lineOrEmpty.
         |_root_.org.typelevel.paiges.Doc.lineOrSpace.
         |_root_.org.typelevel.paiges.Doc.maxSpaceTable.
         |_root_.org.typelevel.paiges.Doc.orderingAtWidth(I)Lscala/math/Ordering;.
         |_root_.org.typelevel.paiges.Doc.paragraph(Ljava/lang/String;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.space.
         |_root_.org.typelevel.paiges.Doc.spaceArray.
         |_root_.org.typelevel.paiges.Doc.spaces(I)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.split(Ljava/lang/String;Lscala/util/matching/Regex;Lorg/typelevel/paiges/Doc;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.splitWhitespace.
         |_root_.org.typelevel.paiges.Doc.spread(Lscala/collection/Iterable;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.stack(Lscala/collection/Iterable;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.str(Ljava/lang/Object;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.str(Ljava/lang/Object;)Lorg/typelevel/paiges/Doc;.T#
         |_root_.org.typelevel.paiges.Doc.tabulate(CLjava/lang/String;Lscala/collection/Iterable;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.tabulate(Lscala/collection/immutable/List;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Doc.text(Ljava/lang/String;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Document#
         |_root_.org.typelevel.paiges.Document#$init$()V.
         |_root_.org.typelevel.paiges.Document#contramap(Lscala/Function1;)Lorg/typelevel/paiges/Document;.
         |_root_.org.typelevel.paiges.Document#contramap(Lscala/Function1;)Lorg/typelevel/paiges/Document;.Z#
         |_root_.org.typelevel.paiges.Document#document(Ljava/lang/Object;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Document.
         |_root_.org.typelevel.paiges.Document.FromToString.
         |_root_.org.typelevel.paiges.Document.FromToString.document(Ljava/lang/Object;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Document.apply(Lorg/typelevel/paiges/Document;)Lorg/typelevel/paiges/Document;.
         |_root_.org.typelevel.paiges.Document.apply(Lorg/typelevel/paiges/Document;)Lorg/typelevel/paiges/Document;.A#
         |_root_.org.typelevel.paiges.Document.documentBoolean.
         |_root_.org.typelevel.paiges.Document.documentByte.
         |_root_.org.typelevel.paiges.Document.documentChar.
         |_root_.org.typelevel.paiges.Document.documentDouble.
         |_root_.org.typelevel.paiges.Document.documentFloat.
         |_root_.org.typelevel.paiges.Document.documentInt.
         |_root_.org.typelevel.paiges.Document.documentIterable(Ljava/lang/String;Lorg/typelevel/paiges/Document;)Lorg/typelevel/paiges/Document;.
         |_root_.org.typelevel.paiges.Document.documentIterable(Ljava/lang/String;Lorg/typelevel/paiges/Document;)Lorg/typelevel/paiges/Document;.A#
         |_root_.org.typelevel.paiges.Document.documentLong.
         |_root_.org.typelevel.paiges.Document.documentShort.
         |_root_.org.typelevel.paiges.Document.documentString.
         |_root_.org.typelevel.paiges.Document.documentUnit.
         |_root_.org.typelevel.paiges.Document.instance(Lscala/Function1;)Lorg/typelevel/paiges/Document;.
         |_root_.org.typelevel.paiges.Document.instance(Lscala/Function1;)Lorg/typelevel/paiges/Document;.A#
         |_root_.org.typelevel.paiges.Document.useToString()Lorg/typelevel/paiges/Document;.
         |_root_.org.typelevel.paiges.Document.useToString()Lorg/typelevel/paiges/Document;.A#
         |_root_.org.typelevel.paiges.DocumentTest#
         |_root_.org.typelevel.paiges.DocumentTest#`<init>`()V.
         |_root_.org.typelevel.paiges.DocumentTest#document(Ljava/lang/Object;Lorg/typelevel/paiges/Document;)Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.DocumentTest#document(Ljava/lang/Object;Lorg/typelevel/paiges/Document;)Lorg/typelevel/paiges/Doc;.A#
         |_root_.org.typelevel.paiges.DocumentTest#generatorDrivenConfig.
         |_root_.org.typelevel.paiges.Generators.
         |_root_.org.typelevel.paiges.Generators.arbDoc.
         |_root_.org.typelevel.paiges.Generators.asciiString.
         |_root_.org.typelevel.paiges.Generators.cogenDoc.
         |_root_.org.typelevel.paiges.Generators.combinators.
         |_root_.org.typelevel.paiges.Generators.doc0Gen.
         |_root_.org.typelevel.paiges.Generators.folds.
         |_root_.org.typelevel.paiges.Generators.genDoc.
         |_root_.org.typelevel.paiges.Generators.genTree(I)Lorg/scalacheck/Gen;.
         |_root_.org.typelevel.paiges.Generators.generalString.
         |_root_.org.typelevel.paiges.Generators.maxDepth.
         |_root_.org.typelevel.paiges.Generators.unary.
         |_root_.org.typelevel.paiges.Json#
         |_root_.org.typelevel.paiges.Json#`<init>`()V.
         |_root_.org.typelevel.paiges.Json#toDoc()Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Json.
         |_root_.org.typelevel.paiges.Json.JArray#
         |_root_.org.typelevel.paiges.Json.JArray#`<init>`(Lscala/collection/immutable/Vector;)V.
         |_root_.org.typelevel.paiges.Json.JArray#toDoc()Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Json.JBool#
         |_root_.org.typelevel.paiges.Json.JBool#`<init>`(Z)V.
         |_root_.org.typelevel.paiges.Json.JBool#toDoc()Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Json.JNull.
         |_root_.org.typelevel.paiges.Json.JNull.toDoc()Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Json.JNumber#
         |_root_.org.typelevel.paiges.Json.JNumber#`<init>`(D)V.
         |_root_.org.typelevel.paiges.Json.JNumber#toDoc()Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Json.JObject#
         |_root_.org.typelevel.paiges.Json.JObject#`<init>`(Lscala/collection/immutable/Map;)V.
         |_root_.org.typelevel.paiges.Json.JObject#toDoc()Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Json.JString#
         |_root_.org.typelevel.paiges.Json.JString#`<init>`(Ljava/lang/String;)V.
         |_root_.org.typelevel.paiges.Json.JString#toDoc()Lorg/typelevel/paiges/Doc;.
         |_root_.org.typelevel.paiges.Json.escape(Ljava/lang/String;)Ljava/lang/String;.
         |_root_.org.typelevel.paiges.JsonTest#
         |_root_.org.typelevel.paiges.JsonTest#`<init>`()V.
         |_root_.org.typelevel.paiges.PaigesTest#
         |_root_.org.typelevel.paiges.PaigesTest#`<init>`()V.
         |_root_.org.typelevel.paiges.PaigesTest#generatorDrivenConfig.
         |_root_.org.typelevel.paiges.PaigesTest.
         |_root_.org.typelevel.paiges.PaigesTest.EquivSyntax#
         |_root_.org.typelevel.paiges.PaigesTest.EquivSyntax#`<init>`(Lorg/typelevel/paiges/Doc;)V.
         |_root_.org.typelevel.paiges.PaigesTest.EquivSyntax#`===`(Lorg/typelevel/paiges/Doc;)Z.
         |_root_.org.typelevel.paiges.PaigesTest.docEquiv.
         |_root_.org.typelevel.paiges.PaigesTest.docEquiv.$anon#equiv(Lorg/typelevel/paiges/Doc;Lorg/typelevel/paiges/Doc;)Z.
         |_root_.org.typelevel.paiges.package.
         |_root_.org.typelevel.paiges.package.call(Ljava/lang/Object;Lscala/collection/immutable/List;)Ljava/lang/Object;.
         |_root_.org.typelevel.paiges.package.call(Ljava/lang/Object;Lscala/collection/immutable/List;)Ljava/lang/Object;.A#
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

}
