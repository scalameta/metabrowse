package metadoc

import scala.scalajs.js
import org.scalajs.dom

object MetadocApp extends js.JSApp {
  def main(): Unit = {
    /*
     * Load the Monaco Editor AMD bundle dynamically since it's incompatible
     * with Webpack: https://github.com/Microsoft/monaco-editor/issues/18
     */
    js.Dynamic.global.require(js.Array("vs/editor/editor.main"), { ctx: MonacoLoaderContext =>
      println("Monaco Editor loaded")
      openEditor(ctx.monaco)
    }: js.ThisFunction)
  }

  def openEditor(monaco: Monaco): Unit = {
    val app = dom.document.getElementById("editor")
    app.innerHTML = ""
    monaco.languages.register(ScalaLanguageExtensionPoint)
    monaco.languages.setMonarchTokensProvider(ScalaLanguageExtensionPoint.id, ScalaLanguage.language)
    monaco.languages.setLanguageConfiguration(ScalaLanguageExtensionPoint.id, ScalaLanguage.conf)
    val editor = monaco.editor.create(app, MonacoEditor.IEditorConstructionOptions(
      value =
        """package example
          |
          |import scala.scalajs.js
          |import scala.scalajs.js.annotation._
          |
          |/**
          | * An editor.
          | *
          | * @see https://microsoft.github.io/monaco-editor/api
          | */
          |object MonacoEditor {
          |  // A list
          |  type A[T] = List[T]
          |
          |  case class IEditorConstructionOptions(
          |    @(JSExport @field) value: js.UndefOr[String]    = js.undefined,
          |    @(JSExport @field) language: js.UndefOr[String] = js.undefined)
          |  )
          |
          |  val Stuff = List("a", 'b', 3.14, 120L, 'sym)
          |  var MutateMe: Map[String, Any] = Map.empty
          |
          |  def isStuff(a: Any): Boolean = a match {
          |    case "a"            => true
          |    case d if d == 3.14 => true
          |    case _              => false
          |  }
          |}
          |""".stripMargin,
      language = "scala"
    ))

    dom.window.onresize = { _: dom.UIEvent => editor.layout() }
  }
}
