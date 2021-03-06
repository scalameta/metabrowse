package metabrowse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import monaco.Range
import monaco.Promise
import monaco.Uri
import monaco.editor.IActionDescriptor
import monaco.editor.IEditor
import monaco.editor.IEditorConstructionOptions
import monaco.editor.IEditorOverrideServices
import monaco.editor.IStandaloneCodeEditor
import monaco.services.IResourceInput
import monaco.services.IEditorService
import org.scalajs.dom

class MetabrowseEditorService(index: MetabrowseSemanticdbIndex)
    extends IEditorService {
  private lazy val editor: IStandaloneCodeEditor = {
    val app = dom.document.getElementById("editor")
    app.innerHTML = ""
    val options = jsObject[IEditorConstructionOptions]
    options.readOnly = true
    options.scrollBeyondLastLine = false

    val overrides = jsObject[IEditorOverrideServices]
    overrides("textModelService") = MetabrowseTextModelService
    overrides("editorService") = this

    val editor = monaco.editor.Editor.create(app, options, overrides)
    editor.asInstanceOf[js.Dynamic].getControl = { () =>
      // NOTE: getControl() is defined on SimpleEditor and is called when changing files.
      editor
    }

    editor
  }

  def addAction(action: IActionDescriptor): Unit =
    editor.addAction(action)

  def resize(): Unit =
    editor.layout()

  def open(input: IResourceInput): Future[IStandaloneCodeEditor] = {
    val selection = input.options.selection
    for {
      MetabrowseMonacoDocument(document, model) <- MetabrowseTextModelService
        .modelDocument(
          input.resource
        )
    } yield {
      editor.setModel(model.`object`.textEditorModel)
      index.dispatch(MetabrowseEvent.SetDocument(document))
      selection.foreach { irange =>
        val range = Range.lift(irange)
        editor.setSelection(range)
        editor.revealPositionInCenter(range.getStartPosition())
        editor.focus()
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
