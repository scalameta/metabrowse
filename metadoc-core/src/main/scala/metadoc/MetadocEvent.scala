package metadoc

import org.langmeta.internal.semanticdb.{schema => s}

sealed abstract class MetadocEvent
object MetadocEvent {
  case class SetDocument(document: s.Document) extends MetadocEvent
}
