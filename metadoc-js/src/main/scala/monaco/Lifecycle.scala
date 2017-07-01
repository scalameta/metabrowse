package monaco

import scala.scalajs.js
import js.annotation._
import js.|

package common {

  @ScalaJSDefined
  trait IDisposable extends js.Object {
    def dispose(): Unit
  }

  @ScalaJSDefined
  trait IReference[T] extends IDisposable {
    def `object`: T
  }

}
