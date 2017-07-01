package metadoc

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.meta.Attributes
import metadoc.schema.Index
import monaco.{CancellationToken, Position}
import monaco.editor.IReadOnlyModel
import monaco.languages.{Location, ReferenceContext, ReferenceProvider}

@ScalaJSDefined
class ScalaReferenceProvider(attrs: Attributes, index: Index)
    extends ReferenceProvider {
  override def provideReferences(
      model: IReadOnlyModel,
      position: Position,
      context: ReferenceContext,
      token: CancellationToken
  ) = {
    val offset = model.getOffsetAt(position)
    val positions = IndexLookup.findReferences(
      offset,
      context.includeDeclaration,
      attrs,
      index
    )
    val locations = positions.map(resolveLocation(model))
    js.Array[Location](locations: _*)
  }
}
