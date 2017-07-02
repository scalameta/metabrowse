package metadoc

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.ScalaJSDefined
import monaco.IPosition
import monaco.Position
import monaco.Promise
import monaco.editor.Editor
import monaco.editor.IEditor
import monaco.editor.IEditorModel
import monaco.editor.IEditorService
import monaco.editor.IModel
import monaco.editor.IResourceInput
import org.scalameta.logger

@ScalaJSDefined
class MetadocEditorService extends IEditorService {
  var editor: IEditor = _
  override def openEditor(
      input: IResourceInput,
      sideBySide: js.UndefOr[Boolean]
  ): Promise[IEditor] = {
    val selection = input.options.selection
    logger.elem(JSON.stringify(selection), input)
    val model = Editor.getModel(input.resource)
    editor.setModel(model)
    selection.foreach { range =>
      val pos = new Position(range.startLineNumber, range.startColumn)
      editor.setPosition(pos)
      editor.revealPositionInCenter(pos)
    }
    Future.successful(editor).toMonacoPromise
  }

  def findModel(model: IModel, data: IResourceInput): IEditorModel = {
    if (model.uri.toString() != data.resource.toString()) {
      null
    } else {
      model
    }
  }
}
