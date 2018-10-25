package metabrowse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import monaco.editor.ITextModel
import monaco.languages.DefinitionProvider
import monaco.languages.Languages.Definition
import monaco.languages.Location
import monaco.CancellationToken
import monaco.Position

class ScalaDefinitionProvider(index: MetabrowseSemanticdbIndex)
    extends DefinitionProvider {
  override def provideDefinition(
      model: ITextModel,
      position: Position,
      token: CancellationToken
  ) = {
    val offset = model.getOffsetAt(position).toInt
    for (symbol <- index.fetchSymbol(offset)) yield {
      val locationOpt = symbol.flatMap(_.definition).map(resolveLocation)
      js.Array[Location](locationOpt.toSeq: _*): Definition
    }
  }.toMonacoThenable
}
