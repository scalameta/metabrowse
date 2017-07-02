package metadoc

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.meta.internal.semantic.{schema => s}
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.ScalaJSDefined
import monaco.Uri
import monaco.common.IReference
import monaco.editor.Editor
import monaco.editor.IModel
import monaco.services.ITextModelResolverService
import org.scalameta.logger
import scala.scalajs.js.|._
import monaco.Promise
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
  ): js.Promise[IReference[IModel]] = {
    val exisingModel = Editor.getModel(resource)
    logger.elem(exisingModel)
    if (exisingModel != null) {
      js.Promise.resolve[IReference[IModel]](IReference(exisingModel))
    } else {
      val path = resource.path
      logger.elem("YEAH!!!!", path)
      val future: Future[IReference[IModel]] = for {
        bytes <- MetadocApp.fetchBytes(MetadocApp.url(resource.path))
      } yield {
        val attrs = s.Attributes.parseFrom(bytes)
        val model = Editor.createModel(attrs.contents, "scala", resource)
        new IReference[IModel] {
          override def `object`: IModel = {
            logger.elem(model)
            model
          }
          override def dispose(): Unit = ()
        }
      }
      future.onComplete { model =>
        logger.elem(model)
      }
      future.toJSPromise
    }
  }
}
