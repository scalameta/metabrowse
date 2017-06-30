package metadoc

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.meta.{Attributes, Denotation}
import metadoc.schema.{Index, Position, Symbol}
import monaco.CancellationToken
import monaco.editor.IReadOnlyModel
import monaco.languages.{DocumentSymbolProvider, SymbolInformation, SymbolKind}

@ScalaJSDefined
class ScalaDocumentSymbolProvider(attrs: Attributes)
    extends DocumentSymbolProvider {
  override def provideDocumentSymbols(
      model: IReadOnlyModel,
      token: CancellationToken
  ) = {
    val denotations = attrs.denotations.map { case (s, d) => s -> d }.toMap
    val symbols = for {
      (symPos, sym) <- attrs.names
      denotation <- denotations.get(sym)
      kind <- symbolKind(denotation)
    } yield {
      val pos = Position("???", symPos.start.offset, symPos.end.offset)
      new SymbolInformation(
        name = denotation.name,
        containerName = "???",
        kind = kind,
        location = resolveLocation(model)(pos)
      )
    }
    js.Array[SymbolInformation](symbols: _*)
  }

  def symbolKind(denotation: Denotation): Option[SymbolKind] = {
    if (denotation.isVal || denotation.isVar)
      Some(SymbolKind.Variable)
    else if (denotation.isDef)
      Some(SymbolKind.Function)
    else if (denotation.isPrimaryCtor || denotation.isSecondaryCtor)
      Some(SymbolKind.Constructor)
    else if (denotation.isClass)
      Some(SymbolKind.Class)
    else if (denotation.isObject)
      Some(SymbolKind.Module)
    else if (denotation.isTrait)
      Some(SymbolKind.Interface)
    else if (denotation.isPackage || denotation.isPackageObject)
      Some(SymbolKind.Package)
    else
      None
  }
}
