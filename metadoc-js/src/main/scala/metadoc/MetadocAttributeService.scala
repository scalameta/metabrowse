package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.langmeta._
import org.langmeta.internal.semanticdb.{schema => s}
import org.langmeta.internal.semanticdb._
import metadoc.MetadocApp._
import metadoc.{schema => d}

object MetadocAttributeService {
  def fetchSymbol(symbolId: String): Future[Option[d.Symbol]] = {
    val url = "symbol/" + JSSha512.sha512(symbolId)
    for {
      bytes <- fetchBytes(url)
    } yield {
      Some(d.Symbol.parseFrom(bytes))
    }
  }.recover {
    case _: NoSuchElementException => None // 404, not found
  }

  def fetchProtoDocument(filename: String): Future[s.Document] = {
    val url = "semanticdb/" + filename.replace(".scala", ".semanticdb")
    for {
      bytes <- fetchBytes(url)
    } yield {
      s.Database.parseFrom(bytes).documents.head
    }
  }

  def fetchDocument(filename: String): Future[Document] = {
    for {
      sdoc <- fetchProtoDocument(filename)
    } yield {
      val db = s.Database(List(sdoc)).toDb(None)
      db.documents.head
    }
  }
}
