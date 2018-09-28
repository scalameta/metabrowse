package mbrowse

import scala.meta.internal.{semanticdb => s}

sealed abstract class MbrowseEvent
object MbrowseEvent {
  case class SetDocument(document: s.TextDocument) extends MbrowseEvent
}
