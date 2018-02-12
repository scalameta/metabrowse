package metadoc

import scala.concurrent.Future
import metadoc.{schema => d}
import org.{langmeta => m}
import scala.meta.internal.{semanticdb3 => s}
import scala.meta.internal.semanticdb3.SymbolOccurrence.{Role => r}

/** Index to lookup symbol definitions and references. */
trait MetadocSemanticdbIndex {
  def document: s.TextDocument
  def symbol(sym: String): Future[Option[d.SymbolIndex]]
  def semanticdb(sym: String): Future[Option[s.TextDocument]]
  def dispatch(event: MetadocEvent): Unit

  def definition(symbol: String): Option[d.Position] =
    document.occurrences.collectFirst {
      case s.SymbolOccurrence(
          Some(s.Range(startLine, startCharacter, endLine, endCharacter)),
          `symbol`,
          r.DEFINITION
          ) =>
        d.Position(
          document.uri,
          startLine,
          startCharacter,
          endLine,
          endCharacter
        )
    }

  def resolve(line: Int, character: Int): Option[s.SymbolOccurrence] = {
    // TODO(olafur) binary search.
    document.occurrences.collectFirst {
      case name @ s.SymbolOccurrence(Some(pos), _, _)
          if pos.startLine <= line && line <= pos.endLine && pos.startCharacter <= character && character <= pos.endCharacter =>
        name
    }
  }

  def fetchSymbol(line: Int, character: Int): Future[Option[d.SymbolIndex]] =
    resolve(line, character).fold(
      Future.successful(Option.empty[d.SymbolIndex])
    ) {
      case s.SymbolOccurrence(_, sym, _) =>
        m.Symbol(sym) match {
          case m.Symbol.Global(_, _) =>
            symbol(sym)
          case _ =>
            // Resolve from local open document.
            val occs = document.occurrences.filter(_.symbol == sym)
            val definition = occs.collectFirst {
              case s.SymbolOccurrence(
                  Some(
                    s.Range(startLine, startCharacter, endLine, endCharacter)
                  ),
                  _,
                  r.DEFINITION
                  ) =>
                d.Position(
                  document.uri,
                  startLine,
                  startCharacter,
                  endLine,
                  endCharacter
                )
            }
            val references = Map(
              document.uri -> d.Ranges(
                occs.collect {
                  case s.SymbolOccurrence(
                      Some(
                        s.Range(
                          startLine,
                          startCharacter,
                          endLine,
                          endCharacter
                        )
                      ),
                      _,
                      r.REFERENCE
                      ) =>
                    d.Range(startLine, startCharacter, endLine, endCharacter)
                }
              )
            )
            val dsymbol = d.SymbolIndex(sym, definition, references)
            Future.successful(Some(dsymbol))
        }
    }
}
