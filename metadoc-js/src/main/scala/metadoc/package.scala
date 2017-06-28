import scala.scalajs.js

package object metadoc {

  /**
    * Instantiate a JavaScript object conforming to a
    * given facade. Main usage is to create an empty
    * object and update its mutable fields.
    *
    * @example
    * {{{
    * @js.native
    * trait Point extends js.Object {
    *   var x: Int = js.native
    *   var y: Int = js.native
    * }
    *
    * val point = jsObject[Point]
    * point.x = 42
    * point.y = 21
    * }}}
    */
  def jsObject[T <: js.Object]: T =
    (new js.Object()).asInstanceOf[T]
}
