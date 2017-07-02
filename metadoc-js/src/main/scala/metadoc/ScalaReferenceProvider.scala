package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.meta.Attributes
import metadoc.{schema => d}
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
      if id.isDefined
      symbol <- MetadocAttributeService.fetchSymbol(id.get)
    } yield {
      val positions = symbol.references
      val locations = positions.map(resolveLocation(model))
      js.Array[Location](locations: _*)
    }
  }.toMonacoThenable
}
