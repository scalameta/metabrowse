package metadoc

import scala.scalajs.js
import org.scalajs.dom

object MetadocApp extends js.JSApp {
  def main(): Unit = {
    val app = dom.document.getElementById("editor")
    app.textContent = "Loading editor ..."

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
    monaco.editor.create(app, MonacoEditor.IEditorConstructionOptions(
      value =
        """const f = (x) => x + 1
          |const g = (x, y) => x * y
          |
          |g(f(2), f(4)) // => 15""".stripMargin,
      language = "javascript"
    ))
  }

  final val ScalaLanguageExtensionPoint = MonacoLanguages.ILanguageExtensionPoint(id = "scala")
}
