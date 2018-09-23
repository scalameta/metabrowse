package metadoc

import scala.util.Try
import scala.scalajs.js
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

  def fromHistoryState(browserState: js.Any): Option[Navigation.State] = {
    Option(browserState) match {
      case Some(state) =>
        Navigation.parseState(state.asInstanceOf[String])
      case None => None
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
        val startLine = fromLine.toInt
        val startColumn = Option(fromCol).map(_.toInt).getOrElse(1)
        val endLine = Option(toLine).map(_.toInt).getOrElse(startLine)
        val endColumn = Option(toCol).map(_.toInt).getOrElse {
          if (endLine == startLine)
            startColumn
          else
            1
        }

        Some(new Selection(startLine, startColumn, endLine, endColumn))
      case _ =>
        None
    }
  }

}
