package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.meta.internal.{semanticdb3 => s}
import metadoc.MetadocApp._
import metadoc.{schema => d}

object MetadocFetch {

  def symbol(symbolId: String): Future[Option[d.SymbolIndex]] = {
    val url = "symbol/" + JSSha512.sha512(symbolId)
    for {
      bytes <- fetchBytes(url)
    } yield {
      Some(d.SymbolIndex.parseFrom(bytes))
    }
  }.recover(or404)

  def document(filename: String): Future[Option[s.TextDocument]] = {
    val url = "semanticdb/" + filename + ".semanticdb"
    for {
      bytes <- fetchBytes(url)
    } yield {
      Some(s.TextDocuments.parseFrom(bytes).documents.head)
    }
  }.recover(or404)

  def workspace(): Future[d.Workspace] = {
    for {
      bytes <- fetchBytes("index.workspace")
    } yield {
      d.Workspace.parseFrom(bytes)
    }
  }

  private def or404[T]: PartialFunction[Throwable, Option[T]] = {
    case _: NoSuchElementException => None // 404, not found
  }

}
