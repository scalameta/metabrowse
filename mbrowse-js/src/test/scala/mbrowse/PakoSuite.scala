package mbrowse

import mbrowse.schema.Workspace
import org.scalatest.FunSuite
import scala.meta.internal.io.JSModules
import scala.meta.internal.io.PathIO
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.scalajs.js.typedarray.TypedArrayBuffer
import scala.scalajs.js.typedarray.Uint8Array

class PakoSuite extends FunSuite {
  test("deflate") {
    val path = PathIO.workingDirectory
      .resolve("target")
      .resolve("mbrowse")
      .resolve("index.workspace.gz")
    val in = path.readAllBytes
    val input = new ArrayBuffer(in.length)
    val bbuf = TypedArrayBuffer.wrap(input)
    bbuf.put(in)
    val output = Pako.inflate(input)
    val out = Array.ofDim[Byte](output.byteLength)
    TypedArrayBuffer.wrap(output).get(out)
    val workspace = Workspace.parseFrom(out)
    val obtained =
      workspace.toProtoString.lines.toList.sorted.mkString("\n").trim
    val expected =
      """
        |filenames: "paiges/core/src/main/scala/org/typelevel/paiges/Chunk.scala"
        |filenames: "paiges/core/src/main/scala/org/typelevel/paiges/Doc.scala"
        |filenames: "paiges/core/src/main/scala/org/typelevel/paiges/Document.scala"
        |filenames: "paiges/core/src/main/scala/org/typelevel/paiges/package.scala"
        |filenames: "paiges/core/src/test/scala/org/typelevel/paiges/DocumentTests.scala"
        |filenames: "paiges/core/src/test/scala/org/typelevel/paiges/Generators.scala"
        |filenames: "paiges/core/src/test/scala/org/typelevel/paiges/JsonTest.scala"
        |filenames: "paiges/core/src/test/scala/org/typelevel/paiges/PaigesTest.scala"
      """.stripMargin.trim
    assert(obtained == expected)
  }
}