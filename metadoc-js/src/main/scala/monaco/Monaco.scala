package monaco

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.|
import scala.scalajs.js.annotation._
import scala.annotation.meta.field
import org.scalajs.dom

/**
 * Main entry point for the Monaco Editor API.
 *
 * Use [[Monaco.load]] to instanciate the API.
 */
@js.native
trait Monaco extends js.Object {
  val editor: monaco.editor.MonacoEditor
  val languages: monaco.languages.MonacoLanguages
}

object Monaco {
  /**
   * The context provided when loading the Monaco Editor bundle with `require`.
   */
  @js.native
  private trait LoaderContext extends js.Object {
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
