package monaco
package editor

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.annotation.meta.field
import org.scalajs.dom

@js.native
trait MonacoEditor extends js.Object {
  def create(
    element: dom.Element,
    options: js.UndefOr[IEditorConstructionOptions] = js.undefined,
    `override`: js.UndefOr[IEditorOverrideServices] = js.undefined
  ): IStandaloneCodeEditor = js.native
}

@ScalaJSDefined
trait IEditorConstructionOptions extends js.Object {
  val value: js.UndefOr[String] = js.undefined
  val language: js.UndefOr[String] = js.undefined
  val readOnly: js.UndefOr[Boolean] = js.undefined
}

case class IDimension(
  @(JSExport @field) height: Int,
  @(JSExport @field) width: Int
)

@js.native
trait IEditorOverrideServices extends js.Object

@js.native
trait IStandaloneCodeEditor extends js.Object {
  def layout(dimension: js.UndefOr[IDimension] = js.undefined): Unit = js.native
}

/**
 * https://microsoft.github.io/monaco-editor/api/interfaces/editor.ireadonlymodel.html
 */
@js.native
trait IReadOnlyModel extends js.Object {
  def uri: Uri = js.native
  def getOffsetAt(position: Position): Int = js.native
  def getPositionAt(offset: Int): Position = js.native
}
