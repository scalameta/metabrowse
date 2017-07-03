package metadoc

import scala.meta.Attributes
import metadoc.schema.{Index, Position, Symbol, Range}

object IndexLookup {
  def findDefinition(
      offset: Int,
      attrs: Attributes,
      index: Index
  ): Option[Position] =
    findSymbol(offset, attrs, index).flatMap(_.definition)

  def findReferences(
      offset: Int,
      includeDeclaration: Boolean,
      attrs: Attributes,
      index: Index,
      filename: String
  ): Seq[Position] =
    findSymbol(offset, attrs, index).toSeq.flatMap {
      case Symbol(_, definition, references) =>
        references
          .get(filename)
          .toSeq
          .flatMap(
            _.ranges.map(r => Position(filename, Some(r)))
          ) ++
          definition.filter(_ => includeDeclaration)
    }

  def findSymbol(
      offset: Int,
      attrs: Attributes,
      index: Index
  ): Option[Symbol] =
    for {
      name <- attrs.names.collectFirst {
        case (pos, sym)
            if pos.start.offset <= offset && offset <= pos.end.offset =>
          sym.syntax
      }
      symbol <- index.symbols.find(_.symbol == name)
    } yield symbol
}
