package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import org.langmeta.internal.semanticdb.{schema => s}
import scala.scalajs.js
import scala.scalajs.js.typedarray.TypedArrayBuffer
import monaco.Uri
import monaco.languages.ILanguageExtensionPoint
import monaco.services.{IResourceInput, ITextEditorOptions}
import org.langmeta.internal.semanticdb.schema.Database
import org.scalajs.dom
import org.scalajs.dom.Event
import metadoc.{schema => d}
import org.langmeta.internal.semanticdb.schema.Document
import org.langmeta.semanticdb.ResolvedName

case class MetadocState(document: s.Document)

class MetadocRoot(init: MetadocState) extends MetadocIndex {
  // TODO(olafur) find way to avoid mutating root.
  var state: MetadocState = init
  def definition(symbol: String): Option[d.Position] =
    state.document.names.collectFirst {
      case s.ResolvedName(Some(s.Position(start, end)), `symbol`, true) =>
        d.Position(state.document.filename, start, end)
    }
  override def document: Document = state.document
  override def symbol(sym: String): Future[Option[schema.Symbol]] =
    MetadocAttributeService.fetchSymbol(sym)
  override def semanticdb(sym: String): Future[Option[s.Document]] =
    MetadocAttributeService.fetchProtoDocument(sym)
}

object MetadocApp {
  def main(args: Array[String]): Unit = {
    for {
      _ <- loadMonaco()
      Some(document) <- MetadocAttributeService
        .fetchProtoDocument(
          "paiges/core/src/main/scala/org/typelevel/paiges/Doc.scala"
        )
    } {
      val root = new MetadocRoot(MetadocState(document))
      registerLanguageExtensions(root)
      val editorService = new MetadocEditorService(root)
      val input = parseResourceInput(document.filename)
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

  def registerLanguageExtensions(root: MetadocRoot): Unit = {
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
      new ScalaDefinitionProvider(root)
    )
    monaco.languages.Languages.registerReferenceProvider(
      ScalaLanguageExtensionPoint.id,
      new ScalaReferenceProvider(root)
    )
    monaco.languages.Languages.registerDocumentSymbolProvider(
      ScalaLanguageExtensionPoint.id,
      new ScalaDocumentSymbolProvider(root)
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
