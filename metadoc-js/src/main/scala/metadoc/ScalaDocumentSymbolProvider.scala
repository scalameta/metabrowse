package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.annotation._
import monaco.CancellationToken
import monaco.editor.IReadOnlyModel
import monaco.languages.DocumentSymbolProvider
import monaco.languages.SymbolInformation
import monaco.languages.SymbolKind
import org.langmeta.internal.semanticdb.{schema => s}
import org.{langmeta => m}
import metadoc.{schema => d}

case class Info(
    denotation: m.Denotation,
    kind: SymbolKind,
    definition: d.Position
)

class ScalaDocumentSymbolProvider(root: MetadocRoot)
    extends DocumentSymbolProvider {

  private def getInfos(doc: s.Document): Seq[Info] = {
    val denotations = doc.symbols.collect {
      case s.ResolvedSymbol(s, Some(d)) =>
        s -> m.Denotation(d.flags, d.name, d.signature, Nil)
    }.toMap
    val infos = for {
      name <- root.state.document.names
      if name.isDefinition
      symbol = m.Symbol(name.symbol)
      if symbol.isInstanceOf[m.Symbol.Global]
      denotation <- denotations.get(name.symbol)
      kind <- symbolKind(denotation)
      definition <- root.definition(name.symbol)
    } yield Info(denotation, kind, definition)
    infos
  }

  override def provideDocumentSymbols(
      model: IReadOnlyModel,
      token: CancellationToken
  ) = {
    for {
      Some(doc) <- root.semanticdb(model.uri.path)
    } yield {
      val symbols = getInfos(doc).map {
        case Info(denotation, kind, definition) =>
          new SymbolInformation(
            name = denotation.name,
            containerName = denotation.signature,
            kind = kind,
            location = model.resolveLocation(definition)
          )
      }
      js.Array[SymbolInformation](symbols: _*)
    }
  }.toMonacoThenable

  def symbolKind(denotation: m.Denotation): Option[SymbolKind] = {
    import denotation._

    if (isParam || isTypeParam)
      None
    else if (isVal || isVar)
      Some(SymbolKind.Variable)
    else if (isDef)
      Some(SymbolKind.Function)
    else if (isPrimaryCtor || isSecondaryCtor)
      Some(SymbolKind.Constructor)
    else if (isClass)
      Some(SymbolKind.Class)
    else if (isObject)
      Some(SymbolKind.Object)
    else if (isTrait)
      Some(SymbolKind.Interface)
    else if (isPackage || isPackageObject)
      Some(SymbolKind.Package)
    else if (isType)
      Some(SymbolKind.Namespace) // Note: no type related symbol kind exists
    else
      None
  }
}
