package metabrowse

import metabrowse.{schema => d}
import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.semanticdb.Scala._

object MetabrowseEnrichments {
  implicit class XtensionMetabrowseRange(val r: d.Range) extends AnyVal {
    def toPosition(uri: String): d.Position = {
      d.Position(
        uri,
        r.startLine,
        r.startCharacter,
        r.endLine,
        r.endCharacter
      )
    }
  }
  implicit class XtensionSymbolString(val symbol: String) extends AnyVal {
    def symbolIndexPath: String = toplevelPackage + "/package.symbolindexes.gz"
    def toplevelPackage: String =
      if (symbol.isNone) symbol
      else if (symbol.isPackage) symbol
      else symbol.owner.toplevelPackage
  }
  implicit class XtensionSemanticdbRange(val r: s.Range) extends AnyVal {
    def toDocRange: d.Range = {
      d.Range(
        r.startLine,
        r.startCharacter,
        r.endLine,
        r.endCharacter
      )
    }
    def toPosition(uri: String): d.Position = {
      d.Position(
        uri,
        r.startLine,
        r.startCharacter,
        r.endLine,
        r.endCharacter
      )
    }
  }

}
