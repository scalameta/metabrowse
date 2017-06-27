package metadoc

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.|
import scala.scalajs.js.annotation._
import scala.annotation.meta.field
import org.scalajs.dom

@JSGlobal
@js.native
abstract class Monaco extends js.Object {
  val editor: MonacoEditor
  val languages: MonacoLanguages
}

object Monaco {
  @js.native
  @JSGlobal("monaco.IDisposable")
  abstract class IDisposable extends js.Object {
    def dispose(): Unit = js.native
  }

  @js.native
  @JSGlobal("monaco.Position")
  class Position extends js.Object {
    def this(lineNumber: Int, column: Int) = this()
    def column: Int = js.native
    def lineNumber: Int = js.native
  }

  @js.native
  @JSGlobal("monaco.Range")
  class Range extends js.Object {
    def this(startLineNumber: Int, startColumn: Int, endLineNumber: Int, endColumn: Int) = this()
    def startColumn: Int = js.native
    def startLineNumber: Int = js.native
    def endColumn: Int = js.native
    def endLineNumber: Int = js.native
  }

  @js.native
  trait CancellationToken extends js.Object {
    def isCancellationRequested: Boolean = js.native
  }

  @js.native
  trait Uri extends js.Object

  /**
   * The context provided when loading the Monaco Editor bundle with `require`.
   */
  @ScalaJSDefined
  abstract class LoaderContext extends js.Object {
    val monaco: Monaco
  }

  /**
   * Load the Monaco Editor AMD bundle using `require`.
   *
   * The AMD bundle is not compatible with Webpack and must be loaded
   * dynamically at runtime to avoid errors:
   * https://github.com/Microsoft/monaco-editor/issues/18
   */
  def load(): Future[Monaco] = {
    val promise = Promise[Monaco]()
    js.Dynamic.global.require(js.Array("vs/editor/editor.main"), { ctx: LoaderContext =>
      println("Monaco Editor loaded")
      promise.success(ctx.monaco)
    }: js.ThisFunction)
    promise.future
  }
}

@js.native
@JSGlobal
abstract class MonacoEditor extends js.Object {
  def create(
    element: dom.Element,
    options: js.UndefOr[MonacoEditor.IEditorConstructionOptions] = js.undefined,
    `override`: js.UndefOr[MonacoEditor.IEditorOverrideServices] = js.undefined
  ): MonacoEditor.IStandaloneCodeEditor = js.native
}

object MonacoEditor {
  @ScalaJSDefined
  trait IEditorConstructionOptions extends js.Object {
    val value: js.UndefOr[String] = js.undefined
    val language: js.UndefOr[String] = js.undefined
    val readOnly: js.UndefOr[Boolean] = js.undefined
  }

  case class IDimension(
    @(JSExport @field) height: Int,
    @(JSExport @field) width: Int
  )

  @js.native
  trait IEditorOverrideServices extends js.Object

  @js.native
  trait IStandaloneCodeEditor extends js.Object {
    def layout(dimension: js.UndefOr[IDimension] = js.undefined): Unit = js.native
  }

  /**
   * https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.ireadonlymodel.html
   */
  @js.native
  trait IReadOnlyModel extends js.Object {
    def uri: Monaco.Uri = js.native
    def getOffsetAt(position: Monaco.Position): Int = js.native
    def getPositionAt(offset: Int): Monaco.Position = js.native
  }
}

@js.native
@JSGlobal
abstract class MonacoLanguages extends js.Object {
  def register(language: MonacoLanguages.ILanguageExtensionPoint): Unit = js.native
  def setMonarchTokensProvider(
    languageId: String,
    languageDef: MonacoLanguages.IMonarchLanguage): Monaco.IDisposable = js.native
  def setLanguageConfiguration(
    languageId: String,
    configuration: MonacoLanguages.LanguageConfiguration): Monaco.IDisposable = js.native
  def registerDefinitionProvider(
    languageId: String,
    provider: MonacoLanguages.DefinitionProvider): Monaco.IDisposable = js.native
}

object MonacoLanguages {
  /**
   * Represents a location inside a resource, such as a line inside a text file.
   */
  case class Location(
    @(JSExport @field) range: Monaco.Range,
    @(JSExport @field) uri: Monaco.Uri
  )

  case class ILanguageExtensionPoint(
    @(JSExport @field) id: String,
    @(JSExport @field) aliases: js.UndefOr[js.Array[String]] = js.undefined,
    @(JSExport @field) configuration: js.UndefOr[String] = js.undefined,
    @(JSExport @field) extensions: js.UndefOr[js.Array[String]] = js.undefined,
    @(JSExport @field) filenamePatterns: js.UndefOr[js.Array[String]] = js.undefined,
    @(JSExport @field) filenames: js.UndefOr[js.Array[String]] = js.undefined,
    @(JSExport @field) firstLine: js.UndefOr[String] = js.undefined,
    @(JSExport @field) mimetypes: js.UndefOr[js.Array[String]] = js.undefined
  )

  /**
   * A Monarch language definition
   *
   * @see https://microsoft.github.io/monaco-editor/api/interfaces/monaco.languages.imonarchlanguage.html
   */
  @js.native
  @JSGlobal("IMonarchLanguage")
  class IMonarchLanguage extends js.Object {
    var tokenPostfix: String = js.native
    var defaultToken: String = js.native
    var tokenizer: js.Object = js.native
  }

  /**
   * The language configuration interface defines the contract between extensions and
   * various editor features, like automatic bracket insertion, automatic indentation etc.
   */
  @js.native
  @JSGlobal("LanguageConfiguration")
  class LanguageConfiguration extends js.Object

  type Definition = Location | js.Array[Location]

  /**
   * The definition provider interface defines the contract between extensions
   * and the go to definition and peek definition features.
   */
  @ScalaJSDefined
  trait DefinitionProvider extends js.Object {
    def provideDefinition(
      model: MonacoEditor.IReadOnlyModel,
      position: Monaco.Position,
      token: Monaco.CancellationToken): Definition
  }
}
