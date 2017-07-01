import scala.scalajs.js
import metadoc.schema.Position
import monaco.editor.IReadOnlyModel
import monaco.languages.Location
import monaco.Range
import monaco.Uri
import org.scalameta.logger

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

  def resolveLocation(model: IReadOnlyModel)(pos: Position) = {
    val startPos = model.getPositionAt(pos.start)
    val endPos = model.getPositionAt(pos.end)
    val range = new Range(
      startPos.lineNumber,
      startPos.column,
      endPos.lineNumber,
      endPos.column
    )
    val uri = Uri.parse(s"file:${pos.filename}")
    logger.elem(uri, uri.path)
    // FIXME: load new file content
    new Location(uri, range)
  }
}
