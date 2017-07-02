package metadoc

import java.net.URLEncoder
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.meta.internal.semantic.{schema => s}
import scala.meta._
import scala.scalajs.js.URIUtils
import metadoc.{schema => d}
import MetadocApp._
import org.scalajs.dom
import org.scalameta.logger

object MetadocAttributeService {
  def fetchSymbol(symbolId: String): Future[d.Symbol] = {
    val url = "symbol/" + dom.window.btoa(symbolId)
    logger.elem(symbolId, url)
    for {
      bytes <- fetchBytes(url)
    } yield {
      d.Symbol.parseFrom(bytes)
    }
  }

  def fetchsAttributes(filename: String): Future[s.Attributes] = {
    for {
      bytes <- fetchBytes(url(filename))
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
