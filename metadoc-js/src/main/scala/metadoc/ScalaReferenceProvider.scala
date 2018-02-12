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

class ScalaReferenceProvider(index: MetadocSemanticdbIndex)
    extends ReferenceProvider {
  override def provideReferences(
      model: IReadOnlyModel,
      position: Position,
      context: ReferenceContext,
      token: CancellationToken
  ) = {
    for {
      sym <- index.fetchSymbol(
        position.lineNumber.toInt,
        position.column.toInt
      )
      locations <- Future.sequence {
        val references = sym.map(_.references).getOrElse(Map.empty)
        references.map {
          case (filename, ranges) =>
            // Create the model for each reference. A reference can come from
            // another file, and we need that file's model in order to get
            // correct range selection.
            MetadocTextModelService
              .modelDocument(createUri(filename))
              .map {
                case MetadocMonacoDocument(_, model) =>
                  ranges.ranges.map { range =>
                    model.`object`.textEditorModel.resolveLocation(
                      schema.Position(
                        filename,
                        range.startLine,
                        range.startCharacter,
                        range.endLine,
                        range.endCharacter
                      )
                    )
                  }
              }
        }
      }
    } yield {
      js.Array[Location](locations.flatten.toSeq: _*)
    }
  }.toMonacoThenable
}
