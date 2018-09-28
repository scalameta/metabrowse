package mbrowse.tests

import caseapp.RemainingArgs
import java.nio.file.Files
import mbrowse.cli.MbrowseCli
import mbrowse.cli.MbrowseOptions
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import scala.meta.io.AbsolutePath
import scala.meta.testkit.DiffAssertions
import mbrowse.{schema => d}
import mbrowse.MbrowseEnrichments._
import GeneratedSiteEnrichments._

class BaseMbrowseCliSuite
    extends FunSuite
    with BeforeAndAfterAll
    with DiffAssertions {
  var out: AbsolutePath = _
  def options = MbrowseOptions(
    Some(out.toString()),
    cleanTargetFirst = true,
    nonInteractive = true
  )
  def files: Seq[String] =
    BuildInfo.exampleClassDirectory.map(_.getAbsolutePath)

  def runCli(): Unit = MbrowseCli.run(options, RemainingArgs(files, Nil))

  override def beforeAll(): Unit = {
    out = AbsolutePath(Files.createTempDirectory("mbrowse"))
    out.toFile.deleteOnExit()
    runCli()
  }

  def checkSymbolIndex(id: String, expected: String) = {
    test(id) {
      val indexes = d.SymbolIndexes.parseFromCompressedPath(
        out.resolve("symbol").resolve(id.symbolIndexPath)
      )
      val index = indexes.indexes.find(_.symbol == id).get
      val obtained = index.toProtoString
      assertNoDiffOrPrintExpected(obtained, expected)
    }
  }

}
