package mbrowse

import scala.concurrent.Future
import scala.meta.internal.semanticdb.TextDocument
import scala.meta.internal.{semanticdb => s}

class MutableBrowserIndex(init: MbrowseState) extends MbrowseSemanticdbIndex {
  private var state: MbrowseState = init

  override def dispatch(event: MbrowseEvent): Unit = event match {
    case MbrowseEvent.SetDocument(document) =>
      state = state.copy(document = document)
  }
  override def document: TextDocument = state.document
  override def symbol(sym: String): Future[Option[schema.SymbolIndex]] =
    MbrowseFetch.symbol(sym)
  override def semanticdb(sym: String): Future[Option[s.TextDocument]] =
    MbrowseFetch.document(sym)
}

case class MbrowseState(document: s.TextDocument)
