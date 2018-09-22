package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import monaco.editor.IReadOnlyModel
import monaco.languages.Location
import monaco.languages.ReferenceContext
import monaco.languages.ReferenceProvider
import monaco.CancellationToken
import monaco.Position
import MetadocEnrichments._

class ScalaReferenceProvider(index: MetadocSemanticdbIndex)
    extends ReferenceProvider {
  override def provideReferences(
      model: IReadOnlyModel,
      position: Position,
      context: ReferenceContext,
      token: CancellationToken
  ) = {
    val offset = model.getOffsetAt(position).toInt
    for {
      sym <- index.fetchSymbol(offset)
    } yield {
      val references = sym.map(_.references).getOrElse(Map.empty)
      val locations = references.flatMap {
        case (filename, ranges) =>
          ranges.ranges.map(_.toPosition(filename)).map(resolveLocation)
      }
      js.Array[Location](locations.toSeq: _*)
    }
  }.toMonacoThenable
}
