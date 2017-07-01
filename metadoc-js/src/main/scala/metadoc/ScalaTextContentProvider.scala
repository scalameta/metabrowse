package metadoc

import scala.scalajs.js.annotation.ScalaJSDefined
import monaco.IDisposable
import monaco.Promise
import monaco.Uri
import monaco.editor.Editor
import monaco.editor.IModel
import monaco.services.ITextModelContentProvider
import monaco.services.ITextModelService

@ScalaJSDefined
class ScalaTextContentProvider extends ITextModelContentProvider {
  override def provideTextContent(resource: Uri): Promise[IModel] = {
    val model = Editor.createModel(
      "object Foo",
      "scala",
      Uri.parse("file:Foo.scala")
    )
    Promise.as(model)
  }
}

