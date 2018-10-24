package metabrowse

import scala.meta.internal.{semanticdb => s}

sealed abstract class MetabrowseEvent
object MetabrowseEvent {
  case class SetDocument(document: s.TextDocument) extends MetabrowseEvent
}
