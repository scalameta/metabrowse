package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBuffer
import scala.scalajs.js.JSConverters._
import monaco.{IRange, Range, Uri}
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

      def defaultState = {
        val state = Navigation.parseState(workspace.filenames.head)
        // Starting with any path, so add the file to the history
        state.foreach(updateHistory)
        state
      }

      /*
       * Discovering the initial input state may update the history so resolve the
       * input before registering the history popstate handler to avoid any event
       * being triggered.
       */
      val state =
        Navigation
          .parseState(Uri.parse(dom.window.location.hash).fragment)
          .orElse(defaultState)

      dom.window.onpopstate = { e: dom.PopStateEvent =>
        for (state <- Navigation.currentState())
          openEditor(editorService)(state)
      }

      dom.window.onresize = { _: dom.Event =>
        editorService.resize()
      }

      state.foreach(openEditor(editorService))
    }
  }

  def updateHistory(state: Navigation.State): Unit = {
    val uri = "#/" + state.toString.dropWhile(_ == '/')

    Navigation.currentState() match {
      case Some(cur) if cur.path == state.path =>
        dom.window.history.replaceState(state, state.path, uri)
      case _ =>
        dom.window.history.pushState(state, state.path, uri)
    }
  }

  def updateTitle(state: Navigation.State): Unit = {
    val title = state.path.dropWhile(_ == '/')
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

  def openEditor(editorService: MetadocEditorService)(
      state: Navigation.State
  ): Unit = {
    val input = jsObject[IResourceInput]
    input.resource = createUri(state.path)
    input.options = jsObject[ITextEditorOptions]
    input.options.selection = state.selection.map(_.toRange).orUndefined

    for (editor <- editorService.open(input)) {
      updateTitle(state)

      editor.onDidChangeCursorSelection { cursor =>
        val selection = Navigation.Selection.fromRange(cursor.selection)
        val state =
          new Navigation.State(editor.getModel().uri.path, Some(selection))
        updateHistory(state)
      }
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
