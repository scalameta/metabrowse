package metadoc.tests

import java.nio.file.Files
import caseapp.RemainingArgs
import org.scalatest.FunSuite
import better.files._
import metadoc.cli.MetadocCli
import metadoc.cli.MetadocOptions

class MetadocCliTest extends FunSuite {
  test("Cli.main") {
    val out = Files.createTempDirectory("metadoc")
    out.toFile.toScala.deleteOnExit()
    val options = MetadocOptions(Some(out.toAbsolutePath.toString))
    val files = BuildInfo.exampleClassDirectory.getAbsolutePath

    // main()
    MetadocCli.run(options, RemainingArgs(List(files), Nil))

    // index()
    assert(Files.exists(out.resolve("metadoc.index")))

    // semanticdb()
    val semanticdb = out.resolve("semanticdb")
    assert(Files.exists(semanticdb))
    val semanticdbs = semanticdb.toFile.list()
    assert(semanticdbs.nonEmpty)
  }
}
