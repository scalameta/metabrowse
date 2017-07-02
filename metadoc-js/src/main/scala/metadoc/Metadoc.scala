package metadoc

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.TypedArrayBuffer
import org.scalajs.dom
import scala.meta._
import metadoc.schema.Index
import monaco.Monaco
import monaco.editor.IEditorConstructionOptions
import monaco.languages.ILanguageExtensionPoint
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import monaco.Uri
import monaco.editor.IEditorModel
import monaco.editor.IEditorOverrideServices
import monaco.editor.IModelChangedEvent

object MetadocApp extends js.JSApp {
  def url(path: String): String =
    "semanticdb/" + path.replace(".scala", ".semanticdb")
  def main(): Unit = {
    for {
      _ <- loadMonaco()
      indexBytes <- fetchBytes("metadoc.index")
      index = Index.parseFrom(indexBytes)
      bytes <- fetchBytes(url(index.files.find(_.endsWith("Doc.scala")).get))
    } {
      val db = Database.load(bytes)
      db.entries.collectFirst {
        case (Input.LabeledString(fileName, contents), attrs) =>
          openEditor(fileName, contents, attrs, index)
      }
    }
  }

  def openEditor(
      fileName: String,
      contents: String,
      attrs: Attributes,
      index: Index
  ): Unit = {
    val app = dom.document.getElementById("editor")
    app.innerHTML = ""
    monaco.languages.Languages.register(ScalaLanguageExtensionPoint)
    monaco.languages.Languages.setMonarchTokensProvider(
      ScalaLanguageExtensionPoint.id,
      ScalaLanguage.language
    )
    monaco.languages.Languages.setLanguageConfiguration(
      ScalaLanguageExtensionPoint.id,
      ScalaLanguage.conf
    )
    monaco.languages.Languages.registerDefinitionProvider(
      ScalaLanguageExtensionPoint.id,
      new ScalaDefinitionProvider(attrs, index)
    )
    monaco.languages.Languages.registerReferenceProvider(
      ScalaLanguageExtensionPoint.id,
      new ScalaReferenceProvider(attrs, index)
    )
    monaco.languages.Languages.registerDocumentSymbolProvider(
      ScalaLanguageExtensionPoint.id,
      new ScalaDocumentSymbolProvider(attrs, index)
    )

    val options = jsObject[IEditorConstructionOptions]
    options.readOnly = true
    val overrides = jsObject[IEditorOverrideServices]
    val textModelResolverService = new MetadocTextModelService
    val editorService = new MetadocEditorService
    overrides.textModelResolverService = textModelResolverService
    overrides.editorService = editorService
    val editor = monaco.editor.Editor.create(app, options, overrides)
    editor.asInstanceOf[js.Dynamic].getControl = { () =>
      // NOTE: getControl() is defined on SimpleEditor.
      editor
    }
    editorService.editor = editor
    editor.onDidChangeModel((arg1: IModelChangedEvent) => {
      val path = arg1.newModelUrl.path
      dom.document.getElementById("title").textContent = path
    })

    val model = monaco.editor.Editor
      .createModel(contents, "scala", monaco.Uri.parse(s"file:$fileName"))
    editor.setModel(model)

    dom.window.addEventListener("resize", (_: dom.Event) => editor.layout())
  }

  def fetchBytes(url: String): Future[Array[Byte]] = {
    for {
      response <- dom.experimental.Fetch.fetch(url).toFuture
      if response.status == 200
      buffer <- response.arrayBuffer().toFuture
    } yield {
      val bytes = Array.ofDim[Byte](buffer.byteLength)
      TypedArrayBuffer.wrap(buffer).get(bytes)
      bytes
    }
  }

  /**
    * Load the Monaco Editor AMD bundle using `require`.
    *
    * The AMD bundle is not compatible with Webpack and must be loaded
    * dynamically at runtime to avoid errors:
    * https://github.com/Microsoft/monaco-editor/issues/18
    */
  def loadMonaco(): Future[Unit] = {
    val promise = Promise[Unit]()
    js.Dynamic.global.require(js.Array("vs/editor/editor.main"), {
      ctx: js.Dynamic =>
        println("Monaco Editor loaded")
        promise.success(())
    }: js.ThisFunction)
    promise.future
  }

  val ScalaLanguageExtensionPoint = new ILanguageExtensionPoint("scala")
}
