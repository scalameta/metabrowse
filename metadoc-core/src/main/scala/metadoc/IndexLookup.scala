package metadoc

import scala.meta.Document
import scala.meta.ResolvedName
import metadoc.schema.{Index, Position, Symbol, Range}

object IndexLookup {
  def findDefinition(
      offset: Int,
      attrs: Document,
      index: Index
  ): Option[Position] =
    findSymbol(offset, attrs, index).flatMap(_.definition)

  def findReferences(
      offset: Int,
      includeDeclaration: Boolean,
      attrs: Document,
      index: Index,
      filename: String
  ): Seq[Position] =
    findSymbol(offset, attrs, index).toSeq.flatMap {
      case Symbol(_, definition, references) =>
        references
          .get(filename)
          .toSeq
          .flatMap(
            _.ranges.map(r => Position(filename, r.start, r.end))
          ) ++
          definition.filter(_ => includeDeclaration)
    }

  def findSymbol(
      offset: Int,
      attrs: Document,
      index: Index
  ): Option[Symbol] =
    for {
      name <- attrs.names.collectFirst {
        case ResolvedName(pos, sym, _)
            if pos.start <= offset && offset <= pos.end =>
          sym.syntax
      }
      symbol <- index.symbols.find(_.symbol == name)
    } yield symbol
}
