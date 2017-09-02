package metadoc

import org.langmeta.Document
import org.langmeta.ResolvedName
import metadoc.schema.{Index, Position, Symbol, Range}

object IndexLookup {
  def findDefinition(
      offset: Int,
      doc: Document,
      index: Index
  ): Option[Position] =
    findSymbol(offset, doc, index).flatMap(_.definition)

  def findReferences(
      offset: Int,
      includeDeclaration: Boolean,
      doc: Document,
      index: Index,
      filename: String
  ): Seq[Position] =
    findSymbol(offset, doc, index).toSeq.flatMap {
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
      doc: Document,
      index: Index
  ): Option[Symbol] =
    for {
      name <- doc.names.collectFirst {
        case ResolvedName(pos, sym, _)
            if pos.start <= offset && offset <= pos.end =>
          sym.syntax
      }
      symbol <- index.symbols.find(_.symbol == name)
    } yield symbol
}
