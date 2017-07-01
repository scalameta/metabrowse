package metadoc

import scala.scalajs.js.Thenable
import scala.scalajs.js.annotation.ScalaJSDefined
import monaco.Promise
import monaco.Uri
import monaco.editor.Editor
import monaco.editor.IEditor
import monaco.editor.IEditorService
import monaco.editor.IResourceInput

@ScalaJSDefined
class MetadocEditorService extends IEditorService {
  var editor: IEditor = _
  override def openEditor(
      input: IResourceInput,
      sideBySide: Boolean
  ): Promise[IEditor] = {
    println(input)
    val model = Editor.createModel(
      "object Foo",
      "scala",
      Uri.parse("foo://bar")
    )
    editor.setModel(model)
    Promise.as(editor)
  }
}
