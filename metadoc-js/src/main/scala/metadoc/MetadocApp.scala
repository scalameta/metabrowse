package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.meta._
import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBuffer
import metadoc.schema.Index
import monaco.editor.IEditor
import monaco.editor.IEditorConstructionOptions
import monaco.editor.IEditorOverrideServices
import monaco.editor.IModelChangedEvent
import monaco.languages.ILanguageExtensionPoint
import org.scalajs.dom
import org.scalajs.dom.Event

object MetadocApp extends js.JSApp {
  def main(): Unit = {
    for {
      _ <- loadMonaco()
      indexBytes <- fetchBytes("metadoc.index")
      index = Index.parseFrom(indexBytes)
    } {
      // 1. Load editor
      val editor = openEditor(index)
      val filename = index.files.find(_.endsWith("Doc.scala")).get
      for {
        attrs <- MetadocAttributeService.fetchsAttributes(filename)
      } yield {
        val model =
          MetadocTextModelService.createModel(attrs.contents, attrs.filename)
        // 2. Open intial file.
        editor.setModel(model)
      }
    }
  }

  def openEditor(index: Index): IEditor = {
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
      new ScalaDefinitionProvider(index)
    )
    monaco.languages.Languages.registerReferenceProvider(
      ScalaLanguageExtensionPoint.id,
      new ScalaReferenceProvider(index)
    )
    monaco.languages.Languages.registerDocumentSymbolProvider(
      ScalaLanguageExtensionPoint.id,
      new ScalaDocumentSymbolProvider(index)
    )

    val options = jsObject[IEditorConstructionOptions]
    options.readOnly = true
    val overrides = jsObject[IEditorOverrideServices]
    val editorService = new MetadocEditorService
    overrides.textModelResolverService = MetadocTextModelService
    overrides.editorService = editorService
    val editor = monaco.editor.Editor.create(app, options, overrides)
    editor.asInstanceOf[js.Dynamic].getControl = { () =>
      // NOTE: getControl() is defined on SimpleEditor and is called when changing files.
      editor
    }
    editorService.editor = editor
    editor.onDidChangeModel((arg1: IModelChangedEvent) => {
      val path = arg1.newModelUrl.path
      dom.document.getElementById("title").textContent = path
      dom.window.location.hash = "#/" + path
    })

    dom.window.onhashchange = { e: Event =>
      val filename = dom.window.location.hash.stripPrefix("#/")
      val uri = createUri(filename)
      for {
        model <- MetadocTextModelService.modelReference(uri)
      } {
        editor.setModel(model.`object`.textEditorModel)
      }
    }
    dom.window.addEventListener("resize", (_: dom.Event) => editor.layout())
    editor
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
