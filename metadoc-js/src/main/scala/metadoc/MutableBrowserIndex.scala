package metadoc

import scala.concurrent.Future
import org.langmeta.internal.semanticdb.schema.Document
import org.langmeta.internal.semanticdb.{schema => s}

class MutableBrowserIndex(init: MetadocState) extends MetadocSemanticdbIndex {
  private var state: MetadocState = init

  override def dispatch(event: MetadocEvent): Unit = event match {
    case MetadocEvent.SetDocument(document) =>
      state = state.copy(document = document)
  }
  override def document: Document = state.document
  override def symbol(sym: String): Future[Option[schema.SymbolIndex]] =
    MetadocFetch.symbol(sym)
  override def semanticdb(sym: String): Future[Option[s.Document]] =
    MetadocFetch.document(sym)
}

case class MetadocState(document: s.Document)
