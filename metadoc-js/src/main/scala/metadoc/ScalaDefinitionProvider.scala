package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.meta._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import metadoc.schema.Index
import monaco.editor.Editor
import monaco.editor.IReadOnlyModel
import monaco.languages.DefinitionProvider
import monaco.languages.Location
import monaco.CancellationToken
import monaco.Position

@ScalaJSDefined
class ScalaDefinitionProvider(index: Index) extends DefinitionProvider {
  override def provideDefinition(
      model: IReadOnlyModel,
      position: Position,
      token: CancellationToken
  ) = {
    val offset = model.getOffsetAt(position)
    for {
      attrs <- MetadocAttributeService.fetchAttributes(model.uri.path)
      locations <- {
        val definition = IndexLookup.findDefinition(offset, attrs, index)
        val locations = definition.map(resolveLocation(model))
        val jsLocations =
          Future.successful(js.Array[Location](locations.toSeq: _*))
        locations.fold(jsLocations) { location =>
          val model = Editor.getModel(location.uri)
          if (model == null) {
            for {
              _ <- MetadocTextModelService.modelReference(location.uri)
              locations <- jsLocations
            } yield locations
          } else jsLocations
        }
      }
    } yield locations
  }.toMonacoThenable
}
