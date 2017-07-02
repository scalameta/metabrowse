import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import metadoc.schema.Position
import monaco.Promise
import monaco.editor.IReadOnlyModel
import monaco.languages.Location
import monaco.Range
import monaco.Thenable
import monaco.Uri
import monaco.editor.IModel

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

  def createModel(value: String, filename: String): IModel =
    monaco.editor.Editor.createModel(value, "scala", createUri(filename))

  def createUri(filename: String): Uri =
    Uri.parse(s"semanticdb:$filename")

  implicit class XtensionFutureToThenable[T](future: Future[T]) {
    import scala.scalajs.js.JSConverters._
    // This method allows us to work with Future[T] in metadoc and convert
    // to monaco.Promise as late as possible.
    def toMonacoPromise: Promise[T] =
      Promise.wrap(future.toJSPromise.asInstanceOf[Thenable[T]])
  }

  def resolveLocation(model: IReadOnlyModel)(pos: Position): Location = {
    val startPos = model.getPositionAt(pos.start)
    val endPos = model.getPositionAt(pos.end)
    val range = new Range(
      startPos.lineNumber,
      startPos.column,
      endPos.lineNumber,
      endPos.column
    )
    val uri = createUri(pos.filename)
    // FIXME: load new file content
    new Location(uri, range)
  }
}
