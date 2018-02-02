package metadoc

import monaco.languages.SymbolKind
import scala.meta.internal.{semanticdb3 => s}
import metadoc.{schema => d}

/** "Go to symbol" eligible definition */
case class DocumentSymbol(
    info: s.SymbolInformation,
    kind: SymbolKind,
    definition: d.Position
)
