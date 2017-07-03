package metadoc

import scala.{meta => m}
import scala.meta.inputs.Input
import scala.meta.Attributes
import scala.meta.Denotation
import scala.scalajs.js
import scala.scalajs.js.annotation._
import metadoc.schema.Index
import monaco.CancellationToken
import monaco.editor.IReadOnlyModel
import monaco.languages.DocumentSymbolProvider
import monaco.languages.SymbolInformation
import monaco.languages.SymbolKind
import scala.concurrent.ExecutionContext.Implicits.global

@ScalaJSDefined
class ScalaDocumentSymbolProvider(index: Index)
    extends DocumentSymbolProvider {
  override def provideDocumentSymbols(
      model: IReadOnlyModel,
      token: CancellationToken
  ) = {
    for {
      attrs <- MetadocAttributeService.fetchAttributes(model.uri.path)
    } yield {
      val denotations = attrs.denotations.map { case (s, d) => s -> d }.toMap
      val symbols = for {
        sym <- index.symbols
        denotation <- denotations.get(m.Symbol(sym.symbol))
        kind <- symbolKind(denotation)
      } yield {
        new SymbolInformation(
          name = denotation.name,
          containerName = denotation.info,
          kind = kind,
          location = model.resolveLocation(sym.definition.get)
        )
      }
      js.Array[SymbolInformation](symbols: _*)
    }
  }.toMonacoThenable

  def symbolKind(denotation: Denotation): Option[SymbolKind] = {
    if (denotation.isPARAM || denotation.isTypeParam)
      None
    else if (denotation.isVal || denotation.isVar)
      Some(SymbolKind.Variable)
    else if (denotation.isDef)
      Some(SymbolKind.Function)
    else if (denotation.isPrimaryCtor || denotation.isSecondaryCtor)
      Some(SymbolKind.Constructor)
    else if (denotation.isClass)
      Some(SymbolKind.Class)
    else if (denotation.isObject)
      Some(SymbolKind.Object)
    else if (denotation.isTrait)
      Some(SymbolKind.Interface)
    else if (denotation.isPackage || denotation.isPackageObject)
      Some(SymbolKind.Package)
    else
      None
  }
}
