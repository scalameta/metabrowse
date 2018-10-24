package metabrowse

import scala.concurrent.Future
import scala.meta.internal.semanticdb.TextDocument
import scala.meta.internal.{semanticdb => s}

class MutableBrowserIndex(init: MetabrowseState) extends MetabrowseSemanticdbIndex {
  private var state: MetabrowseState = init

  override def dispatch(event: MetabrowseEvent): Unit = event match {
    case MetabrowseEvent.SetDocument(document) =>
      state = state.copy(document = document)
  }
  override def document: TextDocument = state.document
  override def symbol(sym: String): Future[Option[schema.SymbolIndex]] =
    MetabrowseFetch.symbol(sym)
  override def semanticdb(sym: String): Future[Option[s.TextDocument]] =
    MetabrowseFetch.document(sym)
}

case class MetabrowseState(document: s.TextDocument)
