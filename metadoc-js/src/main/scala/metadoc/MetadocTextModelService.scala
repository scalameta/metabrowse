package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.meta.internal.semantic.{schema => s}
import scala.scalajs.js.annotation.ScalaJSDefined
import monaco.Promise
import monaco.Uri
import monaco.common.IReference
import monaco.editor.Editor
import monaco.services.ITextEditorModel
import monaco.services.ITextModelResolverService

@ScalaJSDefined
class MetadocTextModelService extends ITextModelResolverService {
  override def createModelReference(
      resource: Uri
  ): Promise[IReference[ITextEditorModel]] = {
    val existingModel = Editor.getModel(resource)
    if (existingModel != null) {
      Promise.as(IReference(ITextEditorModel(existingModel)))
    } else {
      val future = for {
        bytes <- MetadocApp.fetchBytes(MetadocApp.url(resource.path))
      } yield {
        val attrs = s.Attributes.parseFrom(bytes)
        val model = Editor.createModel(attrs.contents, "scala", resource)
        IReference(ITextEditorModel(model))
      }
      future.toMonacoPromise
    }
  }
}
