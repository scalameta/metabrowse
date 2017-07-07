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
import metadoc.{schema => d}
import monaco.editor.IModel

@ScalaJSDefined
class ScalaReferenceProvider(index: Index) extends ReferenceProvider {
  override def provideReferences(
      model: IReadOnlyModel,
      position: Position,
      context: ReferenceContext,
      token: CancellationToken
  ) = {
    val offset = model.getOffsetAt(position).toInt
    for {
      attrs <- MetadocAttributeService.fetchAttributes(model.uri.path)
      id = IndexLookup.findSymbol(offset, attrs, index).map(_.symbol)
      // Monad transformers might come in handy here.
      symbol <- id.fold(Future.successful(Option.empty[d.Symbol]))(
        MetadocAttributeService.fetchSymbol
      )
      locations <- Future.sequence {
        val references = symbol.map(_.references).getOrElse(Seq.empty)
        references.groupBy(_.filename).map {
          case (filename, positions) =>
            // Create the model for each reference. A reference can come from
            // another file, and we need that file's model in order to get
            // correct range selection.
            MetadocTextModelService
              .modelReference(createUri(filename))
              .map { model =>
                positions.map { pos =>
                  model.`object`.textEditorModel.resolveLocation(pos)
                }
              }
        }
      }
    } yield {
      js.Array[Location](locations.flatten.toSeq: _*)
    }
  }.toMonacoThenable
}
