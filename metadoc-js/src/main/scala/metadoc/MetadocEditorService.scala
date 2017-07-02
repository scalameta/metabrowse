package metadoc

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined
import monaco.Promise
import monaco.editor.Editor
import monaco.editor.IEditor
import monaco.editor.IEditorService
import monaco.editor.IResourceInput

@ScalaJSDefined
class MetadocEditorService extends IEditorService {
  var editor: IEditor = _
  override def openEditor(
      input: IResourceInput,
      sideBySide: js.UndefOr[Boolean]
  ): Promise[IEditor] = {
    val model = Editor.getModel(input.resource)
    editor.setModel(model)
    Future.successful(editor).toMonacoPromise
  }
}
