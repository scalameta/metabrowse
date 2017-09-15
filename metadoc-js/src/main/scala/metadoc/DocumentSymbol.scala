package metadoc

import monaco.languages.SymbolKind
import org.{langmeta => m}
import metadoc.{schema => d}

/** "Go to symbol" eligible definition */
case class DocumentSymbol(
    denotation: m.Denotation,
    kind: SymbolKind,
    definition: d.Position
)
