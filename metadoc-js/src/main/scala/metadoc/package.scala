import scala.scalajs.js

package object metadoc {
  /**
   * Instanciate a JavaScript object conforming to a
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
   * build[Point] { (p: Point) =>
   *   p.x = 42
   *   p.y = 21
   * }
   * }}}
   */
  def build[T <: js.Object](f: T => Any): T = {
    val obj = (new js.Object()).asInstanceOf[T]
    f(obj)
    obj
  }
}
