package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined
import monaco.Range
import monaco.Promise
import monaco.editor.Editor
import monaco.editor.IEditor
import monaco.editor.IEditorConstructionOptions
import monaco.editor.IEditorOverrideServices
import monaco.editor.IModelChangedEvent
import monaco.editor.IStandaloneCodeEditor
import monaco.services.IResourceInput
import monaco.services.IEditorService
import org.scalajs.dom

@ScalaJSDefined
class MetadocEditorService extends IEditorService {
  private lazy val editor: IStandaloneCodeEditor = {
    val app = dom.document.getElementById("editor")
    app.innerHTML = ""
    val options = jsObject[IEditorConstructionOptions]
    options.readOnly = true

    val overrides = jsObject[IEditorOverrideServices]
    overrides.textModelResolverService = MetadocTextModelService
    overrides.editorService = this

    val editor = monaco.editor.Editor.create(app, options, overrides)
    editor.asInstanceOf[js.Dynamic].getControl = { () =>
      // NOTE: getControl() is defined on SimpleEditor and is called when changing files.
      editor
    }

    editor
  }

  def open(input: IResourceInput): Future[IStandaloneCodeEditor] = {
    val selection = input.options.selection
    for {
      model <- MetadocTextModelService.modelReference(input.resource)
    } yield {
      editor.setModel(model.`object`.textEditorModel)
      selection.foreach {
        case range: Range =>
          val pos = range.getStartPosition()
          editor.setPosition(pos)
          editor.revealPositionInCenter(pos)
      }
      editor
    }
  }

  override def openEditor(
      input: IResourceInput,
      sideBySide: js.UndefOr[Boolean] = js.undefined
  ): Promise[IEditor] =
    open(input).toMonacoPromise
}
