package metadoc

import metadoc.schema.Workspace
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBuffer
import scala.scalajs.js.JSConverters._
import monaco.{IRange, Range, Uri}
import monaco.Uri
import monaco.editor.Editor
import monaco.editor.IActionDescriptor
import monaco.editor.ICommonCodeEditor
import monaco.editor.IEditor
import monaco.editor.IEditorConstructionOptions
import monaco.editor.IEditorOptions
import monaco.editor.IEditorOverrideServices
import monaco.editor.IModelChangedEvent
import monaco.languages.ILanguageExtensionPoint
import monaco.services.{IResourceInput, ITextEditorOptions}
import org.scalajs.dom
import scala.meta.internal.{semanticdb => s}

object MetadocApp {
  def main(args: Array[String]): Unit = {
    val editorThemeMenuElement =
      dom.document.querySelector("#editor-theme-menu");
    val editorThemeMenu = new mdc.MDCSimpleMenu(editorThemeMenuElement);
    val editorThemeIcon = dom.document.querySelector(".editor-theme");
    editorThemeIcon.addEventListener("click", { (event: dom.Event) =>
      event.preventDefault()
      editorThemeMenu.open = !editorThemeMenu.open
    })

    for (theme <- Seq("vs", "vs-dark", "hc-black")) {
      val themeControl = dom.document
        .getElementById(s"theme:$theme")
        .asInstanceOf[dom.html.Input]

      themeControl.onclick = { (e: dom.MouseEvent) =>
        Editor.setTheme(theme)
        dom.ext.LocalStorage.update("editor-theme", theme)
      }
    }

    for {
      _ <- loadMonaco()
      workspace <- MetadocFetch.workspace()
    } {
      val index = new MutableBrowserIndex(MetadocState(s.TextDocument()))
      registerLanguageExtensions(index)

      val editorService = new MetadocEditorService(index)
      registerWorkspaceFiles(editorService, workspace)

      dom.ext.LocalStorage("editor-theme").foreach { theme =>
        Editor.setTheme(theme)
      }

      def defaultState: Navigation.State =
        new Navigation.State(workspace.filenames.head, None)

      def locationState() =
        Navigation.parseState(Uri.parse(dom.window.location.hash).fragment)

      /*
       * Discovering the initial input state may update the history so resolve the
       * input before registering the history popstate handler to avoid any event
       * being triggered.
       */
      val initialState = locationState().getOrElse(defaultState)

      dom.window.onpopstate = { event: dom.PopStateEvent =>
        Navigation
          .fromHistoryState(event.state)
          .orElse(locationState())
          .foreach(openEditor(editorService))
      }

      dom.window.onresize = { _: dom.Event =>
        editorService.resize()
      }

      openEditor(editorService)(initialState)
    }
  }

  def updateHistory(state: Navigation.State): Unit = {
    val uri = "#/" + state.toString.dropWhile(_ == '/')

    Navigation.fromHistoryState(dom.window.history.state) match {
      case Some(cur) if cur.path == state.path =>
        dom.window.history.replaceState(state.toString, state.path, uri)
      case _ =>
        dom.window.history.pushState(state.toString, state.path, uri)
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

  def registerWorkspaceFiles(
      editorService: MetadocEditorService,
      workspace: Workspace
  ): Unit = {
    workspace.filenames.foreach { file =>
      editorService.addAction(new IActionDescriptor {
        override var id = file
        override var label = file
        override def run(
            editor: ICommonCodeEditor
        ): monaco.Promise[Unit] = {
          val state = new Navigation.State(file, None)
          openEditor(editorService)(state).toMonacoPromise
        }
        override var precondition: String = ""
        override var keybindings: js.Array[Double] = js.Array()
        override var keybindingContext: String = ""
        override var contextMenuGroupId: String = ""
        override var contextMenuOrder: Double = 0
      })
    }
  }

  def openEditor(editorService: MetadocEditorService)(
      state: Navigation.State
  ): Future[Unit] = {
    val input = jsObject[IResourceInput]
    input.resource = createUri(state.path)
    input.options = jsObject[ITextEditorOptions]
    input.options.selection = state.selection.map(_.toRange).orUndefined

    updateHistory(state)

    for (editor <- editorService.open(input)) yield {
      updateTitle(state)

      editor.onDidChangeCursorSelection { cursor =>
        val selection = Navigation.Selection.fromRange(cursor.selection)
        val state =
          new Navigation.State(editor.getModel().uri.path, Some(selection))
        updateTitle(state)
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

  val ScalaLanguageExtensionPoint = {
    val language = jsObject[ILanguageExtensionPoint]
    language.id = "scala"
    language
  }
}
