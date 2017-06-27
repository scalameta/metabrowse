package monaco
package languages

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.annotation.meta.field

@js.native
trait MonacoLanguages extends js.Object {
  def register(language: ILanguageExtensionPoint): Unit = js.native
  def setMonarchTokensProvider(
    languageId: String,
    languageDef: IMonarchLanguage): IDisposable = js.native
  def setLanguageConfiguration(
    languageId: String,
    configuration: LanguageConfiguration): IDisposable = js.native
  def registerDefinitionProvider(
    languageId: String,
    provider: DefinitionProvider): IDisposable = js.native
}

/**
 * Represents a location inside a resource, such as a line inside a text file.
 */
case class Location(
  @(JSExport @field) range: Range,
  @(JSExport @field) uri: Uri
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
@JSGlobal("monaco.languages.IMonarchLanguage")
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
@JSGlobal("monaco.languages.LanguageConfiguration")
class LanguageConfiguration extends js.Object

/**
 * The definition provider interface defines the contract between extensions
 * and the go to definition and peek definition features.
 */
@ScalaJSDefined
trait DefinitionProvider extends js.Object {
  def provideDefinition(
    model: editor.IReadOnlyModel,
    position: Position,
    token: CancellationToken): Definition
}
