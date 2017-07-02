package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.meta._
import scala.meta.internal.semantic.{schema => s}
import metadoc.MetadocApp._
import metadoc.{schema => d}
import org.scalajs.dom

object MetadocAttributeService {
  def fetchSymbol(symbolId: String): Future[Option[d.Symbol]] = {
    val url = "symbol/" + dom.window.btoa(symbolId)
    for {
      bytes <- fetchBytes(url)
    } yield {
      Some(d.Symbol.parseFrom(bytes))
    }
  }.recover {
    case _: NoSuchElementException => None // 404, not found
  }

  def fetchsAttributes(filename: String): Future[s.Attributes] = {
    val url = "semanticdb/" + filename.replace(".scala", ".semanticdb")
    for {
      bytes <- fetchBytes(url)
    } yield {
      s.Attributes.parseFrom(bytes)
    }
  }

  def fetchAttributes(filename: String): Future[Attributes] = {
    for {
      sattrs <- fetchsAttributes(filename)
    } yield {
      val db = s.Database(List(sattrs)).toMeta(None)
      db.entries.head._2
    }
  }
}
