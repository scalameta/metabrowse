package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.langmeta._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import metadoc.schema.Index
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
    val offset = model.getOffsetAt(position).toInt
    for {
      attrs <- MetadocAttributeService.fetchDocument(model.uri.path)
      locations <- {
        val definition = IndexLookup.findDefinition(offset, attrs, index)
        definition.fold(Future.successful(js.Array[Location]())) { defn =>
          for {
            model <- MetadocTextModelService.modelReference(defn.filename)
          } yield {
            val location =
              model.`object`.textEditorModel.resolveLocation(defn)
            js.Array[Location](location)
          }
        }
      }
    } yield locations
  }.toMonacoThenable
}
