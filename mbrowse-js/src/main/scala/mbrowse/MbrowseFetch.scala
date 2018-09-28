package mbrowse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.meta.internal.{semanticdb => s}
import mbrowse.MbrowseApp._
import mbrowse.{schema => d}
import MbrowseEnrichments._

object MbrowseFetch {

  def symbol(symbolId: String): Future[Option[d.SymbolIndex]] = {
    val url = "symbol/" + symbolId.symbolIndexPath
    for {
      bytes <- fetchBytes(url)
    } yield {
      val indexes = d.SymbolIndexes.parseFrom(bytes)
      indexes.indexes.find(_.symbol == symbolId)
    }
  }.recover(or404)

  def document(filename: String): Future[Option[s.TextDocument]] = {
    val url = "semanticdb/" + filename + ".semanticdb.gz"
    for {
      bytes <- fetchBytes(url)
    } yield {
      Some(s.TextDocuments.parseFrom(bytes).documents.head)
    }
  }.recover(or404)

  def workspace(): Future[d.Workspace] = {
    for {
      bytes <- fetchBytes("index.workspace.gz")
    } yield {
      d.Workspace.parseFrom(bytes)
    }
  }

  private def or404[T]: PartialFunction[Throwable, Option[T]] = {
    case _: NoSuchElementException => None // 404, not found
  }

}
