package metadoc

import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
@JSImport("js-sha512", JSImport.Namespace)
object JSSha512 extends js.Object {
  def sha512(text: String): String = js.native
}
