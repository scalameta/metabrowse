package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.meta.internal.semantic.{schema => s}
import scala.scalajs.js.annotation.ScalaJSDefined
import monaco.Promise
import monaco.Uri
import monaco.common.IReference
import monaco.editor.Editor
import monaco.services.ITextEditorModel
import monaco.services.ITextModelResolverService

@ScalaJSDefined
object MetadocTextModelService extends ITextModelResolverService {
  def modelReference(
      resource: Uri
  ): Future[IReference[ITextEditorModel]] = {
    val existingModel = Editor.getModel(resource)
    if (existingModel != null) {
      Future.successful(IReference(ITextEditorModel(existingModel)))
    } else {
      for {
        bytes <- MetadocApp.fetchBytes(MetadocApp.url(resource.path))
      } yield {
        val attrs = s.Attributes.parseFrom(bytes)
        val model = Editor.createModel(attrs.contents, "scala", resource)
        IReference(ITextEditorModel(model))
      }
    }
  }
  override def createModelReference(
      resource: Uri
  ): Promise[IReference[ITextEditorModel]] =
    modelReference(resource).toMonacoPromise
}
