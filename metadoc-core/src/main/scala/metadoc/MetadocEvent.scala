package metadoc

import scala.meta.internal.{semanticdb3 => s}

sealed abstract class MetadocEvent
object MetadocEvent {
  case class SetDocument(document: s.TextDocument) extends MetadocEvent
}
