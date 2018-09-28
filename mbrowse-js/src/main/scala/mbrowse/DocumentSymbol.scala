package mbrowse

import monaco.languages.SymbolKind
import mbrowse.{schema => d}
import scala.meta.internal.semanticdb.SymbolInformation

/** "Go to symbol" eligible definition */
case class DocumentSymbol(
    info: SymbolInformation,
    kind: SymbolKind,
    definition: d.Position
)
