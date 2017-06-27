package metadoc

import scala.scalajs.js
import scala.scalajs.js.annotation._
import org.scalajs.dom
import scala.meta._
import metadoc.schema.{Index, Symbol}

@ScalaJSDefined
class ScalaDefinitionProvider(attrs: Attributes, index: Index) extends MonacoLanguages.DefinitionProvider {
  override def provideDefinition(
    model: MonacoEditor.IReadOnlyModel,
    position: Monaco.Position,
    token: Monaco.CancellationToken): MonacoLanguages.Definition = {

    val offset = model.getOffsetAt(position)

    val symbol: Option[String] = attrs.names.collectFirst {
      case (pos, sym) if pos.start.offset <= offset && offset <= pos.end.offset =>
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

        // FIXME: check definition.filename and reload
        MonacoLanguages.Location(
          range = new Monaco.Range(
            startPos.lineNumber, startPos.column,
            endPos.lineNumber, endPos.column
          ),
          uri = model.uri)
      case None =>
        js.Array[MonacoLanguages.Location]()
    }
  }
}
