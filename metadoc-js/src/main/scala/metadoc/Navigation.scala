package metadoc

import scala.util.Try
import scala.scalajs.js
import org.scalajs.dom
import java.net.URI
import monaco.Range

object Navigation {
  class State(val path: String, val selection: Option[Selection])
      extends js.Object {
    override def toString: String =
      path + selection.map(_.toString).fold("")("#" + _)
  }

  case class Selection(
      startLine: Int,
      startColumn: Int,
      endLine: Int,
      endColumn: Int
  ) {
    def toRange() =
      new Range(startLine, startColumn, endLine, endColumn)

    override def toString: String = {
      def position(lineNumber: Int, column: Int): String =
        s"L$lineNumber${if (column > 1) s"C${column}" else ""}"

      val start = position(startLine, startColumn)
      if (startLine == endLine && startColumn == endColumn)
        start
      else if (startLine == endLine - 1 && startColumn == 1 && endColumn == 1)
        start
      else
        start + "-" + position(endLine, endColumn)
    }
  }

  object Selection {
    val Regex = """L(\d+)(C(\d+))?(-L(\d+)(C(\d+))?)?""".r

    def fromRange(range: Range): Selection =
      new Selection(
        range.startLineNumber.toInt,
        range.startColumn.toInt,
        range.endLineNumber.toInt,
        range.endColumn.toInt
      )
  }

  def currentState(maybeState: js.Any): Option[Navigation.State] = {
    val state = Option(maybeState) match {
      case Some(serialized) =>
        Navigation.parseState(maybeState.asInstanceOf[String])
      case None => None
    }
    state.orElse {
      val locationHash = dom.window.location.hash.dropWhile(_ == '#')
      parseState(locationHash)
    }
  }

  def parseState(state: String): Option[Navigation.State] = {
    for (uri <- parseUri(state)) yield {
      val selection = Option(uri.getFragment).flatMap(parseSelection)
      new State(uri.getPath, selection)
    }
  }

  def parseUri(uri: String): Option[URI] =
    if (uri.isEmpty) None
    else Try(URI.create(uri)).toOption

  def parseSelection(selection: String): Option[Selection] = {
    selection match {
      case Selection.Regex(fromLine, _, fromCol, _, toLine, _, toCol) =>
        val defaultLine = fromLine.toInt
        val defaultColumn = Option(fromCol).map(_.toInt).getOrElse(1)
        Some(
          new Selection(
            defaultLine,
            Option(fromCol).map(_.toInt).getOrElse(1),
            Option(toLine).map(_.toInt).getOrElse(defaultLine),
            Option(toCol).map(_.toInt).getOrElse(defaultColumn)
          )
        )
      case _ =>
        None
    }
  }

}
