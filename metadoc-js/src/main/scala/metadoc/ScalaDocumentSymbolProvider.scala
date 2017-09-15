package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import monaco.CancellationToken
import monaco.editor.IReadOnlyModel
import monaco.languages.DocumentSymbolProvider
import monaco.languages.SymbolInformation
import monaco.languages.SymbolKind
import org.langmeta.internal.semanticdb.{schema => s}
import org.{langmeta => m}

class ScalaDocumentSymbolProvider(index: MetadocSemanticdbIndex)
    extends DocumentSymbolProvider {

  private def getDocumentSymbols(doc: s.Document): Seq[DocumentSymbol] = {
    val denotations = doc.symbols.collect {
      case s.ResolvedSymbol(s, Some(d)) =>
        s -> m.Denotation(d.flags, d.name, d.signature, Nil)
    }.toMap
    val infos = for {
      name <- index.document.names
      if name.isDefinition
      symbol = m.Symbol(name.symbol)
      if symbol.isInstanceOf[m.Symbol.Global]
      denotation <- denotations.get(name.symbol)
      kind <- symbolKind(denotation)
      definition <- index.definition(name.symbol)
    } yield DocumentSymbol(denotation, kind, definition)
    infos
  }

  override def provideDocumentSymbols(
      model: IReadOnlyModel,
      token: CancellationToken
  ) = {
    for {
      Some(doc) <- index.semanticdb(model.uri.path)
    } yield {
      val symbols = getDocumentSymbols(doc).map {
        case DocumentSymbol(denotation, kind, definition) =>
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
