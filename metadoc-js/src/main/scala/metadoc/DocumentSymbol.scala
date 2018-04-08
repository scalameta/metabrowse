package metadoc

import monaco.languages.SymbolKind
import org.{langmeta => m}
import metadoc.{schema => d}
import scala.meta.internal.semanticdb3.SymbolInformation

/** "Go to symbol" eligible definition */
case class DocumentSymbol(
    info: SymbolInformation,
    kind: SymbolKind,
    definition: d.Position
)
