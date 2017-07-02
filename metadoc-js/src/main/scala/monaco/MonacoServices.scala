package monaco

package services {

  import scala.scalajs.js
  import scala.scalajs.js.annotation.ScalaJSDefined
  import monaco.common.IReference
  import monaco.editor.IModel

  @ScalaJSDefined
  trait ITextModelContentProvider extends js.Object {
    def provideTextContent(resource: Uri): Promise[IModel]
  }

  @ScalaJSDefined
  trait ITextModelService extends js.Object {
    def registerTextModelContentProvider(
        scheme: String,
        provider: ITextModelContentProvider
    ): IDisposable
  }

  @ScalaJSDefined
  trait ITextModelResolverService extends js.Object {
    def createModelReference(
        resource: Uri
    ): Promise[IReference[ITextEditorModel]]
  }

  @ScalaJSDefined
  trait ITextEditorModel extends js.Object {
    def textEditorModel: IModel
  }

  object ITextEditorModel {
    def apply(model: IModel) = new ITextEditorModel {
      override def textEditorModel: IModel = model
    }
  }
}
