package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import monaco.CancellationToken
import monaco.editor.IReadOnlyModel
import monaco.languages.DocumentSymbolProvider
import monaco.languages.SymbolInformation
import monaco.languages.SymbolKind
import scala.meta.internal.{semanticdb3 => s}
import org.{langmeta => m}

class ScalaDocumentSymbolProvider(index: MetadocSemanticdbIndex)
    extends DocumentSymbolProvider {

  private def getDocumentSymbols(doc: s.TextDocument): Seq[DocumentSymbol] = {
    val infos = doc.symbols.map(info => (info.symbol, info)).toMap
    val documentSymbols = for {
      occ <- index.document.occurrences
      if occ.role.isDefinition
      symbol = m.Symbol(occ.symbol)
      if symbol.isInstanceOf[m.Symbol.Global]
      info <- infos.get(occ.symbol)
      kind <- symbolKind(info)
      definition <- index.definition(occ.symbol)
    } yield DocumentSymbol(info, kind, definition)
    documentSymbols
  }

  override def provideDocumentSymbols(
      model: IReadOnlyModel,
      token: CancellationToken
  ) = {
    for {
      Some(doc) <- index.semanticdb(model.uri.path)
    } yield {
      val symbols = getDocumentSymbols(doc).map {
        case DocumentSymbol(info, kind, definition) =>
          new SymbolInformation(
            name = info.name,
            containerName = info.signature.map(_.text).getOrElse(""),
            kind = kind,
            location = model.resolveLocation(definition)
          )
      }
      js.Array[SymbolInformation](symbols: _*)
    }
  }.toMonacoThenable

  def symbolKind(info: s.SymbolInformation): Option[SymbolKind] = {
    import info.kind._

    if (isParameter || isTypeParameter)
      None
    else if (isVal || isVar)
      Some(SymbolKind.Variable)
    else if (isDef)
      Some(SymbolKind.Function)
    else if (isPrimaryConstructor || isSecondaryConstructor)
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
