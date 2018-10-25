package metabrowse

import monaco.languages.SymbolKind
import metabrowse.{schema => d}
import scala.meta.internal.semanticdb.SymbolInformation

/** "Go to symbol" eligible definition */
case class DocumentSymbol(
    info: SymbolInformation,
    kind: SymbolKind,
    definition: d.Position
)
