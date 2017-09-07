package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import org.langmeta._
import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBuffer
import metadoc.schema.Index
import monaco.Uri
import monaco.languages.ILanguageExtensionPoint
import monaco.services.{IResourceInput, ITextEditorOptions}
import org.scalajs.dom
import org.scalajs.dom.Event

object MetadocApp extends js.JSApp {
  def main(): Unit = {
    for {
      _ <- loadMonaco()
      indexBytes <- fetchBytes("metadoc.index")
    } {
      val index = Index.parseFrom(indexBytes)

      registerLanguageExtensions(index)

      val editorService = new MetadocEditorService()
      val input = parseResourceInput(
        index.files.find(_.endsWith("Doc.scala")).get
      )
      openEditor(editorService, input)
    }
  }

  def parseResourceInput(defaultPath: String): IResourceInput = {
    val path = Option(dom.window.location.hash.stripPrefix("#/"))
      .filter(_.nonEmpty)
      .getOrElse(defaultPath)
    val input = jsObject[IResourceInput]
    input.resource = createUri(path)
    input.options = jsObject[ITextEditorOptions]
    input
  }

  def updateLocation(uri: Uri): Unit = {
    dom.document.getElementById("title").textContent = uri.path
    dom.window.location.hash = "#/" + uri.path
  }

  def registerLanguageExtensions(index: Index): Unit = {
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
  }

  def openEditor(
      editorService: MetadocEditorService,
      input: IResourceInput
  ): Unit = {
    updateLocation(input.resource)
    for (editor <- editorService.open(input)) {
      editor.onDidChangeModel(event => updateLocation(event.newModelUrl))

      dom.window.onhashchange = { e: Event =>
        openEditor(editorService, parseResourceInput(editor.getModel.uri.path))
      }

      dom.window.addEventListener("resize", (_: dom.Event) => editor.layout())
    }
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
