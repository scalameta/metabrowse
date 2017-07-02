package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.meta.Attributes
import metadoc.schema.Index
import monaco.{CancellationToken, Position}
import monaco.editor.IReadOnlyModel
import monaco.languages.{Location, ReferenceContext, ReferenceProvider}

@ScalaJSDefined
class ScalaReferenceProvider(index: Index) extends ReferenceProvider {
  override def provideReferences(
      model: IReadOnlyModel,
      position: Position,
      context: ReferenceContext,
      token: CancellationToken
  ) = {
    val offset = model.getOffsetAt(position)
    for {
      attrs <- MetadocAttributeService.fetchAttributes(model.uri.path)
      id = IndexLookup.findSymbol(offset, attrs, index).map(_.symbol)
      symbol <- MetadocAttributeService.fetchSymbol(id.get)
      references <- {
        val positions = symbol.references.map { reference =>
          // Create the model for each reference. A reference can come from
          // another file, and we need that file's model in order to get
          // correct range selection.
          MetadocTextModelService
            .modelReference(createUri(reference.filename))
            .map(_.`object`.textEditorModel -> reference)
        }
        Future.sequence(positions)
      }
    } yield {
      val locations = references.map {
        case (referenceModel, pos) => resolveLocation(referenceModel)(pos)
      }
      js.Array[Location](locations: _*)
    }
  }.toMonacoThenable
}
