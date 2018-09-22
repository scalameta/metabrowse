package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import monaco.editor.IReadOnlyModel
import monaco.languages.DefinitionProvider
import monaco.languages.Location
import monaco.CancellationToken
import monaco.Position

class ScalaDefinitionProvider(index: MetadocSemanticdbIndex)
    extends DefinitionProvider {
  override def provideDefinition(
      model: IReadOnlyModel,
      position: Position,
      token: CancellationToken
  ) = {
    val offset = model.getOffsetAt(position).toInt
    for (symbol <- index.fetchSymbol(offset)) yield {
      val locationOpt = symbol.flatMap(_.definition).map(resolveLocation)
      js.Array[Location](locationOpt.toSeq: _*)
    }
  }.toMonacoThenable
}
