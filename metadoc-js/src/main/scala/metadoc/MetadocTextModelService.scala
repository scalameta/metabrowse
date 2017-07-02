package metadoc

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.meta.internal.semantic.{schema => s}
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.ScalaJSDefined
import monaco.Promise
import monaco.Thenable
import monaco.Uri
import monaco.common.IReference
import monaco.editor.Editor
import monaco.editor.IModel
import monaco.services.ITextEditorModel
import monaco.services.ITextModelResolverService
import org.scalameta.logger

class MetadocModelHandler {
  private val models = mutable.Map.empty[String, IModel]
  def create(uri: Uri, contents: String): IModel = {
    val model =
      models.getOrElse(uri.path, Editor.createModel(contents, "scala", uri))
    model
  }
}

@ScalaJSDefined
class MetadocTextModelService extends ITextModelResolverService {
  override def createModelReference(
      resource: Uri
  ): Promise[IReference[ITextEditorModel]] = {
    logger.elem(resource)
    val existingModel = Editor.getModel(resource)
    if (existingModel != null) {
      logger.elem(existingModel)
      Promise.as(IReference(ITextEditorModel(existingModel)))
    } else {
      logger.elem("YEAH!!!!", resource.path)
      val future = for {
        bytes <- MetadocApp.fetchBytes(MetadocApp.url(resource.path))
      } yield {
        val attrs = s.Attributes.parseFrom(bytes)
        val model = Editor.createModel(attrs.contents, "scala", resource)
        IReference(ITextEditorModel(model))
      }
      future.onComplete { newModel =>
        logger.elem(newModel)
      }
      Promise.wrap(
        future.toJSPromise.asInstanceOf[Thenable[IReference[ITextEditorModel]]]
      )
    }
  }
}
