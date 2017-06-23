package metadoc

import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
@JSGlobal
object ScalaLanguage extends js.Object {
  val language: MonacoLanguages.IMonarchLanguage = js.native
  val conf: MonacoLanguages.LanguageConfiguration = js.native
}


object ScalaLanguageExtensionPoint extends MonacoLanguages.ILanguageExtensionPoint(id = "scala")
