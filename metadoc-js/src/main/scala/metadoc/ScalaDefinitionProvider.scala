package metadoc

import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom
import scala.meta._
import metadoc.schema.Index
import monaco.{CancellationToken, Position}
import monaco.editor.IReadOnlyModel
import monaco.languages.DefinitionProvider
import monaco.languages.Location

@ScalaJSDefined
class ScalaDefinitionProvider(attrs: Attributes, index: Index)
    extends DefinitionProvider {
  override def provideDefinition(
      model: IReadOnlyModel,
      position: Position,
      token: CancellationToken
  ) = {
    val offset = model.getOffsetAt(position)
    val definition = IndexLookup.findDefinition(offset, attrs, index)
    val locations = definition.map(resolveLocation(model))
    js.Array[Location](locations.toSeq: _*)
  }
}
