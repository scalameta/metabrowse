package metadoc

import metadoc.{schema => d}
import scala.meta.internal.{semanticdb => s}

object MetadocEnrichments {
  implicit class XtensionMetadocRange(val r: d.Range) extends AnyVal {
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
