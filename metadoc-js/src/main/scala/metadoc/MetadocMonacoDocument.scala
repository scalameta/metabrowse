package metadoc

import monaco.services.IReference
import monaco.services.ITextEditorModel
import scala.meta.internal.{semanticdb => s}

/** Logicless wrapper around a metadoc+monaco document/model.
  *
  * @param document Scalameta semanticdb document.
  * @param model Monaco model for a document.
  */
case class MetadocMonacoDocument(
    document: s.TextDocument,
    model: IReference[ITextEditorModel]
)
