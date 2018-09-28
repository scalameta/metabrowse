package scala.meta.internal.mbrowse

import scala.meta.internal.semanticdb.Scala._

object ScalametaInternals {
  def ownerAndDesc(symbol: String): (String, Descriptor) = {
    DescriptorParser(symbol).swap
  }
}
