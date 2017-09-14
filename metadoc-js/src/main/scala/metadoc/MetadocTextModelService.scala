package metadoc

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import monaco.Promise
import monaco.Uri
import monaco.editor.Editor
import monaco.editor.IModel
import monaco.services.IReference
import monaco.services.ITextEditorModel
import monaco.services.ITextModelService
import monaco.services.ImmortalReference
import org.langmeta.internal.semanticdb.{schema => s}

case class EditorDocument(
    document: s.Document,
    model: IReference[ITextEditorModel]
)

object MetadocTextModelService extends ITextModelService {
  def modelReference(
      filename: String
  ): Future[IReference[ITextEditorModel]] =
    modelReference(createUri(filename)).map(_.model)

  private val modelDocument = mutable.Map.empty[IModel, s.Document]

  private def document(model: IModel) =
    EditorDocument(
      modelDocument(model),
      new ImmortalReference(ITextEditorModel(model))
    )

  def modelReference(
      resource: Uri
  ): Future[EditorDocument] = {
    val model = Editor.getModel(resource)
    if (model != null) {
      Future.successful(document(model))
    } else {
      for {
        Some(doc) <- MetadocFetchService.fetchProtoDocument(resource.path)
      } yield {
        val model = Editor.createModel(doc.contents, "scala", resource)
        modelDocument(model) = doc
        document(model)
      }
    }
  }

  override def createModelReference(
      resource: Uri
  ): Promise[IReference[ITextEditorModel]] =
    modelReference(resource).map(_.model).toMonacoPromise
}
