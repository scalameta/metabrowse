package metadoc

import scala.meta.internal.io._
import scala.meta._
import scala.meta.internal.semantic.{vfs => v}
import scala.meta.internal.semantic.{schema => s}
import scala.meta.internal.io.FileIO
import scala.meta.internal.semantic.vfs.Paths
import org.scalameta.logger

object Metadoc {
  val cp = "metadoc/js/target/scala-2.12/classes"
  val sourcepath = "metadoc/shared/src"
  def main(args: Array[String]): Unit = {
    implicit val mirror = Mirror()
    val cp = Classpath(
      PathIO.workingDirectory.resolve("../../example/target/scala-2.12/classes"))
    generateSite(cp)
  }

  def generateSite(cp: Classpath): Unit = {
    v.Database.load(cp).entries.foreach { e =>
      val sattrs = s.Attributes.parseFrom(e.bytes)
      logger.elem(e.fragment, sattrs, sattrs.names, e.fragment.name)
    }
  }
}
