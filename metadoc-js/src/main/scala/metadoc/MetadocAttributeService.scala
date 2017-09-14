package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.langmeta._
import org.langmeta.internal.semanticdb.{schema => s}
import org.langmeta.internal.semanticdb._
import metadoc.MetadocApp._
import metadoc.{schema => d}

object MetadocAttributeService {

  def or404[T]: PartialFunction[Throwable, Option[T]] = {
    case _: NoSuchElementException => None // 404, not found
  }

  def fetchSymbol(symbolId: String): Future[Option[d.Symbol]] = {
    val url = "symbol/" + JSSha512.sha512(symbolId)
    for {
      bytes <- fetchBytes(url)
    } yield {
      Some(d.Symbol.parseFrom(bytes))
    }
  }.recover(or404)

  def fetchProtoDocument(filename: String): Future[Option[s.Document]] = {
    val url = "semanticdb/" + filename + ".semanticdb"
    for {
      bytes <- fetchBytes(url)
    } yield {
      Some(s.Database.parseFrom(bytes).documents.head)
    }
  }.recover(or404)
}
