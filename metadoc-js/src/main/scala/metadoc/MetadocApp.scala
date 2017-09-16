package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBuffer
import monaco.Uri
import monaco.languages.ILanguageExtensionPoint
import monaco.services.{IResourceInput, ITextEditorOptions}
import org.scalajs.dom
import org.langmeta.internal.semanticdb.{schema => s}

object MetadocApp {
  def main(args: Array[String]): Unit = {
    for {
      _ <- loadMonaco()
      workspace <- MetadocFetch.workspace()
    } {
      val index = new MutableBrowserIndex(MetadocState(s.Document()))
      registerLanguageExtensions(index)
      val editorService = new MetadocEditorService(index)

      def defaultInput = {
        val input = parseResourceInput(workspace.filenames.head)
        // Starting with any path, so add the file to the history
        input.foreach(updateHistory)
        input
      }

      /*
       * Discovering the initial input state may update the history so resolve the
       * input before registering the history popstate handler to avoid any event
       * being triggered.
       */
      val input = parseResourceInput(Uri.parse(dom.window.location.hash).fragment)
        .orElse(defaultInput)

      dom.window.onpopstate = { e: dom.PopStateEvent =>
        val input = Option(e.state.asInstanceOf[IResourceInput]).orElse(
          // FIXME: history.replaceState?
          parseResourceInput(Uri.parse(dom.window.location.hash).fragment)
        )
        input.foreach(openEditor(editorService))
      }

      dom.window.addEventListener("resize", (_: dom.Event) => editorService.resize())

      input.foreach(openEditor(editorService))
    }
  }

  def parseResourceInput(location: String): Option[IResourceInput] = {
    Option(location)
      .filter(_.nonEmpty)
      .map { uri =>
        val input = jsObject[IResourceInput]
        input.resource = createUri(uri)
        input.options = jsObject[ITextEditorOptions]
        input
      }
  }

  def updateHistory(input: IResourceInput): Unit = {
    val uri = input.resource
    dom.window.history.pushState(input, uri.path, "#/" + uri.path)
  }

  def updateTitle(input: IResourceInput): Unit = {
    val title = input.resource.path.dropWhile(_ == '/')
    dom.document.getElementById("title").textContent = title
  }

  def registerLanguageExtensions(index: MetadocSemanticdbIndex): Unit = {
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
      editorService: MetadocEditorService)(
      input: IResourceInput
  ): Unit = {
    for (editor <- editorService.open(input)) {
      updateTitle(input)
    }
  }

  def fetchBytes(url: String): Future[Array[Byte]] = {
    for {
      response <- dom.experimental.Fetch.fetch(url).toFuture
      _ = require(response.status == 200, s"${response.status} != 200")
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
