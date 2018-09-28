package mbrowse

import monaco.services.IReference
import monaco.services.ITextEditorModel
import scala.meta.internal.{semanticdb => s}

/** Logicless wrapper around a mbrowse+monaco document/model.
  *
  * @param document Scalameta semanticdb document.
  * @param model Monaco model for a document.
  */
case class MbrowseMonacoDocument(
    document: s.TextDocument,
    model: IReference[ITextEditorModel]
)
