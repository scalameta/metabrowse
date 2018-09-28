package metadoc

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import monaco.Promise
import monaco.Uri
import monaco.editor.Editor
import monaco.editor.ITextModel
import monaco.services.IReference
import monaco.services.ITextEditorModel
import monaco.services.ITextModelService
import monaco.services.ImmortalReference
import scala.meta.internal.{semanticdb => s}

object MetadocTextModelService extends ITextModelService {
  def modelReference(
      filename: String
  ): Future[IReference[ITextEditorModel]] =
    modelDocument(createUri(filename)).map(_.model)

  // TODO(olafur): Move this state out for easier testing.
  private val modelDocumentCache = mutable.Map.empty[ITextModel, s.TextDocument]

  private def document(model: ITextModel) =
    MetadocMonacoDocument(
      modelDocumentCache(model),
      new ImmortalReference(ITextEditorModel(model))
    )

  def modelDocument(
      resource: Uri
  ): Future[MetadocMonacoDocument] = {
    val model = Editor.getModel(resource)
    if (model != null) {
      Future.successful(document(model))
    } else {
      for {
        Some(doc) <- MetadocFetch.document(resource.path)
      } yield {
        val model = Editor.createModel(doc.text, "scala", resource)
        modelDocumentCache(model) = doc
        document(model)
      }
    }
  }

  override def createModelReference(
      resource: Uri
  ): Promise[IReference[ITextEditorModel]] =
    modelDocument(resource).map(_.model).toMonacoPromise
}
