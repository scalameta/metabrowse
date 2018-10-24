package metabrowse.tests
import java.nio.file.Files
import java.util.zip.GZIPInputStream
import scala.meta.io.AbsolutePath
import scalapb.GeneratedMessage
import scalapb.GeneratedMessageCompanion
import scalapb.Message

object GeneratedSiteEnrichments {
  implicit class XtensionGeneratedMessageCompanion[
      A <: GeneratedMessage with Message[A]
  ](companion: GeneratedMessageCompanion[A]) {
    def parseFromCompressedPath(path: AbsolutePath): A = {
      val in = Files.newInputStream(path.toNIO)
      val gin = new GZIPInputStream(in)
      try companion.parseFrom(gin)
      finally {
        gin.close()
        in.close()
      }
    }
  }

}
