package metadoc

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.annotation.ScalaJSDefined
import monaco.Promise
import monaco.Uri
import monaco.editor.Editor
import monaco.editor.IModel
import monaco.services.IReference
import monaco.services.ImmortalReference
import monaco.services.ITextEditorModel
import monaco.services.ITextModelService

@ScalaJSDefined
object MetadocTextModelService extends ITextModelService {
  def modelReference(
      filename: String
  ): Future[IReference[ITextEditorModel]] =
    modelReference(createUri(filename))

  def modelReference(
      resource: Uri
  ): Future[IReference[ITextEditorModel]] = {
    val existingModel = Editor.getModel(resource)
    if (existingModel != null) {
      Future.successful(new ImmortalReference(ITextEditorModel(existingModel)))
    } else {
      for {
        attrs <- MetadocAttributeService.fetchProtoDocument(resource.path)
      } yield {
        val model = Editor.createModel(attrs.contents, "scala", resource)
        new ImmortalReference(ITextEditorModel(model))
      }
    }
  }
  override def createModelReference(
      resource: Uri
  ): Promise[IReference[ITextEditorModel]] =
    modelReference(resource).toMonacoPromise
}
