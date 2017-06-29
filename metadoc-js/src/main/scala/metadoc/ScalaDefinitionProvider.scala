package metadoc

import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom
import scala.meta._
import metadoc.schema.{Index, Symbol}
import monaco.languages.DefinitionProvider
import monaco.languages.Location

@ScalaJSDefined
class ScalaDefinitionProvider(attrs: Attributes, index: Index)
    extends DefinitionProvider {
  override def provideDefinition(
      model: monaco.editor.IReadOnlyModel,
      position: monaco.Position,
      token: monaco.CancellationToken
  ) = {
    val offset = model.getOffsetAt(position)
    val definition = IndexLookup.findDefinition(offset, attrs, index)
    val locations = definition.map(resolveLocation(model))
    js.Array[Location](locations.toSeq: _*)
  }
}
