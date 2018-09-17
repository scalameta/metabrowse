package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import monaco.editor.ITextModel
import monaco.languages.DefinitionLink
import monaco.languages.DefinitionProvider
import monaco.languages.Location
import monaco.CancellationToken
import monaco.Position

class ScalaDefinitionProvider(index: MetadocSemanticdbIndex)
    extends DefinitionProvider {
  override def provideDefinition(
      model: ITextModel,
      position: Position,
      token: CancellationToken
  ) = {
    val offset = model.getOffsetAt(position).toInt
    def empty = Future.successful(js.Array[DefinitionLink]())
    for {
      symbol <- index.fetchSymbol(offset)
      locations <- {
        symbol
          .map(_.definition)
          .fold(empty) {
            case Some(defn) =>
              for {
                model <- MetadocTextModelService.modelReference(defn.filename)
              } yield {
                val location =
                  model.`object`.textEditorModel.resolveLocation(defn)
                val definitionLink = jsObject[DefinitionLink]
                //definitionLink.origin = location.range
                definitionLink.uri = location.uri
                definitionLink.range = location.range
                definitionLink.selectionRange = location.range
                js.Array[DefinitionLink](definitionLink)
              }
            case None => empty
          }
      }
    } yield locations
  }.toMonacoThenable
}
