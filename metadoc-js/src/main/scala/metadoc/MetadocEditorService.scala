package metadoc

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined
import monaco.Range
import monaco.Promise
import monaco.editor.Editor
import monaco.editor.IEditor
import monaco.services.IResourceInput
import monaco.services.IEditorService

@ScalaJSDefined
class MetadocEditorService extends IEditorService {
  var editor: IEditor = _
  override def openEditor(
      input: IResourceInput,
      sideBySide: js.UndefOr[Boolean]
  ): Promise[IEditor] = {
    val selection = input.options.selection
    val model = Editor.getModel(input.resource)
    editor.setModel(model)
    selection.foreach {
      case range: Range =>
        val pos = range.getStartPosition()
        editor.setPosition(pos)
        editor.revealPositionInCenter(pos)
    }
    Future.successful(editor).toMonacoPromise
  }
}
