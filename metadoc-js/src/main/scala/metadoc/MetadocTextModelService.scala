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
import monaco.editor.IEditor
import monaco.editor.IEditorService
import monaco.editor.IModel
import monaco.editor.IResourceInput
import monaco.services.ITextEditorModel
import monaco.services.ITextModelResolverService
import org.scalameta.logger

@ScalaJSDefined
class MetadocEditorService extends IEditorService {
  var editor: IEditor = _
  override def openEditor(
      input: IResourceInput,
      sideBySide: js.UndefOr[Boolean]
  ): Promise[IEditor] = {
    logger.elem(input.resource)
    val model = Editor.getModel(input.resource)
    editor.setModel(model)
    Future.successful(editor).toMonacoPromise
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
      future.toMonacoPromise
    }
  }
}
