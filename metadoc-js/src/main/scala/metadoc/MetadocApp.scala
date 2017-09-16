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
      val input =
        parseResourceInput(Uri.parse(dom.window.location.hash).fragment)
          .orElse(defaultInput)

      dom.window.onpopstate = { e: dom.PopStateEvent =>
        val input = Option(e.state.asInstanceOf[IResourceInput]).orElse(
          parseResourceInput(Uri.parse(dom.window.location.hash).fragment)
        )
        input.foreach(openEditor(editorService))
      }

      dom.window.onresize = { _: dom.Event =>
        editorService.resize()
      }

      input.foreach(openEditor(editorService))
    }
  }

  val SelectionRegex = """L(\d+)(C(\d+))?(-L(\d+)(C(\d+))?)?""".r

  def parseResourceInput(location: String): Option[IResourceInput] = {
    Option(location)
      .filter(_.nonEmpty)
      .map { location =>
        val uri = Uri.parse(location)
        val selection = Option(uri.fragment).flatMap(parseSelection)
        createInputResource(uri, selection)
      }
  }

  def parseSelection(selection: String): Option[Range] = {
    selection match {
      case SelectionRegex(fromLine, _, fromCol, _, toLine, _, toCol) =>
        Some(
          new Range(
            fromLine.toInt,
            Option(fromCol).map(_.toDouble).getOrElse(1),
            Option(toLine).map(_.toDouble).getOrElse(fromLine.toInt + 1),
            Option(toCol).map(_.toDouble).getOrElse(1)
          )
        )
      case _ =>
        None
    }
  }

  def selectionFragment(range: IRange): String = {
    def position(lineNumber: Int, column: Int): String =
      s"L$lineNumber${if (column > 1) s"C${column}" else ""}"

    val start = position(range.startLineNumber.toInt, range.startColumn.toInt)
    if (range.startLineNumber == range.endLineNumber && range.startColumn == range.endColumn)
      start
    else if (range.startLineNumber == range.endLineNumber - 1 && range.startColumn == 1 && range.endColumn == 1)
      start
    else
      start + "-" + position(range.endLineNumber.toInt, range.endColumn.toInt)
  }

  def updateHistory(input: IResourceInput): Unit = {
    val uri = input.resource
    val selection = input.options.selection.toOption
    val fragment = selection.map(selectionFragment).fold("")("#" + _)
    val location = "#/" + uri.path.dropWhile(_ == '/') + fragment

    val currentState = dom.window.history.state.asInstanceOf[IResourceInput]
    val currentInput = Option(currentState).orElse(
      parseResourceInput(Uri.parse(dom.window.location.hash).fragment)
    )

    currentInput match {
      case Some(cur) if cur.resource.path == input.resource.path =>
        dom.window.history.replaceState(input, uri.path, location)
      case _ =>
        dom.window.history.pushState(input, uri.path, location)
    }
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

  def openEditor(editorService: MetadocEditorService)(
      input: IResourceInput
  ): Unit = {
    for (editor <- editorService.open(input)) {
      updateTitle(input)

      editor.onDidChangeCursorSelection { cursor =>
        val selection = Some(cursor.selection)
        val input = createInputResource(editor.getModel().uri, selection)
        updateHistory(input)
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
