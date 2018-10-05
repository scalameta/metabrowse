package metadoc.tests

import caseapp.RemainingArgs
import java.nio.file.Files
import metadoc.cli.MetadocCli
import metadoc.cli.MetadocOptions
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import scala.meta.io.AbsolutePath
import scala.meta.testkit.DiffAssertions
import metadoc.{schema => d}
import metadoc.MetadocEnrichments._
import GeneratedSiteEnrichments._

class BaseMetadocCliSuite
    extends FunSuite
    with BeforeAndAfterAll
    with DiffAssertions {
  var out: AbsolutePath = _
  def options = MetadocOptions(
    Some(out.toString()),
    cleanTargetFirst = true,
    nonInteractive = true
  )
  def files: Seq[String] =
    BuildInfo.exampleClassDirectory.map(_.getAbsolutePath)

  def runCli(): Unit = MetadocCli.run(options, RemainingArgs(files, Nil))

  override def beforeAll(): Unit = {
    out = AbsolutePath(Files.createTempDirectory("metadoc"))
    out.toFile.deleteOnExit()
    runCli()
  }

  def checkSymbolIndex(id: String, expected: String) = {
    test(id) {
      val indexes = d.SymbolIndexes.parseFromCompressedPath(
        out.resolve("symbol").resolve(id.symbolIndexPath)
      )
      val index = indexes.indexes.find(_.symbol == id).get
      // Sort ranges to ensure we assert against deterministic input.
      val indexNormalized = index.copy(
        references = index.references.mapValues { ranges =>
          ranges.copy(ranges = ranges.ranges.sortBy(_.startLine))
        }
      )
      val obtained = indexNormalized.toProtoString
      assertNoDiffOrPrintExpected(obtained, expected)
    }
  }

}
