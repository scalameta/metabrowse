package metabrowse

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.typedarray.ArrayBuffer

@js.native
@JSImport("pako", JSImport.Default)
object Pako extends js.Object {
  def deflate(input: ArrayBuffer): ArrayBuffer = js.native
  def deflateRaw(input: ArrayBuffer): ArrayBuffer = js.native
  def inflate(input: ArrayBuffer): ArrayBuffer = js.native
  def inflateRaw(input: ArrayBuffer): ArrayBuffer = js.native
}
