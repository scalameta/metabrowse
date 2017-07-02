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
    def createModelReference(resource: Uri): js.Promise[IReference[IModel]]
  }
}
