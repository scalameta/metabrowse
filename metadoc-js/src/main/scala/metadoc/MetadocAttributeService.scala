package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.meta.internal.semantic.{schema => s}
import scala.meta._
import MetadocApp._

object MetadocAttributeService {
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
