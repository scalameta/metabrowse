package metadoc

import scala.concurrent.Future
import metadoc.{schema => d}
import org.{langmeta => m}
import org.langmeta.internal.semanticdb.{schema => s}
import scala.concurrent.ExecutionContext.Implicits.global

trait MetadocIndex {
  def document: s.Document
  def resolve(offset: Int): Option[s.ResolvedName] =
    document.names.collectFirst {
      case name @ s.ResolvedName(Some(pos), _, _)
          if pos.start <= offset && offset <= pos.end =>
        name
    }
  def fetchSymbol(offset: Int): Future[Option[d.Symbol]] =
    resolve(offset).fold(Future.successful(Option.empty[d.Symbol])) {
      case s.ResolvedName(_, sym, _) =>
        m.Symbol(sym) match {
          case m.Symbol.Global(_, _) =>
            symbol(sym)
          case _ =>
            // resolve from active document
            val names = document.names.filter(_.symbol == sym)
            val dsymbol = d.Symbol(
              sym,
              definition = names.collectFirst {
                case s.ResolvedName(Some(s.Position(start, end)), _, true) =>
                  d.Position(document.filename, start, end)
              },
              references = Map(
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
            )
            Future.successful(Some(dsymbol))
        }

    }
  def symbol(sym: String): Future[Option[d.Symbol]]
  def semanticdb(sym: String): Future[Option[s.Document]]
}
