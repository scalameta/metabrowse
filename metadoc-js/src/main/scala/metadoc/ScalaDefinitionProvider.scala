package metadoc

import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom
import scala.meta._
import metadoc.schema.{Index, Symbol}
import monaco.languages.DefinitionProvider
import monaco.languages.Location

@ScalaJSDefined
class ScalaDefinitionProvider(attrs: Attributes, index: Index)
    extends DefinitionProvider {
  override def provideDefinition(
      model: monaco.editor.IReadOnlyModel,
      position: monaco.Position,
      token: monaco.CancellationToken
  ) = {

    val offset = model.getOffsetAt(position)

    val symbol: Option[String] = attrs.names.collectFirst {
      case (pos, sym)
          if pos.start.offset <= offset && offset <= pos.end.offset =>
        sym.syntax
    }
    val maybeDefinition = symbol.flatMap { sym =>
      index.symbols.collectFirst {
        case Symbol(name, Some(definition), _) if sym == name =>
          definition
      }
    }

    maybeDefinition match {
      case Some(definition) =>
        val startPos = model.getPositionAt(definition.start)
        val endPos = model.getPositionAt(definition.end)
        val range = new monaco.Range(
          startPos.lineNumber,
          startPos.column,
          endPos.lineNumber,
          endPos.column
        )

        // FIXME: check definition.filename and reload
        new Location(model.uri, range)
      case None =>
        js.Array[Location]()
    }
  }
}
