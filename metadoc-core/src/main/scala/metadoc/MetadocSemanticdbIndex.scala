package metadoc

import scala.concurrent.Future
import metadoc.{schema => d}
import org.{langmeta => m}
import org.langmeta.internal.semanticdb.{schema => s}

/** Index to lookup symbol definitions and references. */
trait MetadocSemanticdbIndex {
  def document: s.Document
  def symbol(sym: String): Future[Option[d.SymbolIndex]]
  def semanticdb(sym: String): Future[Option[s.Document]]
  def dispatch(event: MetadocEvent): Unit

  def definition(symbol: String): Option[d.Position] =
    document.names.collectFirst {
      case s.ResolvedName(Some(s.Position(start, end)), `symbol`, true) =>
        d.Position(document.filename, start, end)
    }

  def resolve(offset: Int): Option[s.ResolvedName] = {
    // TODO(olafur) binary search.
    document.names.collectFirst {
      case name @ s.ResolvedName(Some(pos), _, _)
          if pos.start <= offset && offset <= pos.end =>
        name
    }
  }

  def fetchSymbol(offset: Int): Future[Option[d.SymbolIndex]] =
    resolve(offset).fold(Future.successful(Option.empty[d.SymbolIndex])) {
      case s.ResolvedName(_, sym, _) =>
        m.Symbol(sym) match {
          case m.Symbol.Global(_, _) =>
            symbol(sym)
          case _ =>
            // Resolve from local open document.
            val names = document.names.filter(_.symbol == sym)
            val definition = names.collectFirst {
              case s.ResolvedName(Some(s.Position(start, end)), _, true) =>
                d.Position(document.filename, start, end)
            }
            val references = Map(
              document.filename -> d.Ranges(
                names.collect {
                  case s.ResolvedName(
                      Some(s.Position(start, end)),
                      _,
                      false
                      ) =>
                    d.Range(start, end)
                }
              )
            )
            val dsymbol = d.SymbolIndex(sym, definition, references)
            Future.successful(Some(dsymbol))
        }
    }
}
