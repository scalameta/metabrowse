package metabrowse.tests

import caseapp.RemainingArgs
import java.nio.file.Files
import metabrowse.cli.MetabrowseCli
import metabrowse.cli.MetabrowseOptions
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import scala.meta.io.AbsolutePath
import metabrowse.{schema => d}
import metabrowse.MetabrowseEnrichments._
import GeneratedSiteEnrichments._

abstract class BaseMetabrowseCliSuite
    extends FunSuite
    with BeforeAndAfterAll
    with DiffAssertions {
  var out: AbsolutePath = _
  def options = MetabrowseOptions(
    out.toString(),
    cleanTargetFirst = true,
    nonInteractive = true
  )
  def files: Seq[String] =
    BuildInfo.exampleClassDirectory.map(_.getAbsolutePath).toSeq

  def runCli(): Unit = MetabrowseCli.run(options, RemainingArgs(files, Nil))

  override def beforeAll(): Unit = {
    out = AbsolutePath(Files.createTempDirectory("metabrowse"))
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
        references = index.references
          .mapValues { ranges =>
            ranges.copy(ranges = ranges.ranges.sortBy(_.startLine))
          }
          .iterator
          .toMap
      )
      val obtained = indexNormalized.toProtoString
      assertNoDiffOrPrintExpected(obtained, expected)
    }
  }

}
