package metadoc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import monaco.CancellationToken
import monaco.editor.ITextModel
import monaco.languages.{DocumentSymbol => MonacoDocumentSymbol}
import monaco.languages.DocumentSymbolProvider
import monaco.languages.SymbolKind
import scala.meta.internal.{semanticdb => s}
import scala.{meta => m}
import scala.meta.internal.semanticdb.Scala._

class ScalaDocumentSymbolProvider(index: MetadocSemanticdbIndex)
    extends DocumentSymbolProvider {

  override var displayName = "Metadoc Scala Symbol Provider"

  private def getDocumentSymbols(doc: s.TextDocument): Seq[DocumentSymbol] = {
    val denotations = doc.symbols.map { info =>
      info.symbol -> info
    }.toMap
    val infos = for {
      name <- index.document.occurrences
      if name.role.isDefinition
      if name.symbol.isGlobal
      denotation <- denotations.get(name.symbol)
      kind <- symbolKind(denotation)
      definition <- index.definition(name.symbol)
    } yield DocumentSymbol(denotation, kind, definition)
    infos
  }

  override def provideDocumentSymbols(
      model: ITextModel,
      token: CancellationToken
  ) = {
    for {
      Some(doc) <- index.semanticdb(model.uri.path)
    } yield {
      val symbols = getDocumentSymbols(doc).map {
        case DocumentSymbol(denotation, kind, definition) =>
          val symbol = jsObject[MonacoDocumentSymbol]
          symbol.name = denotation.displayName
          // TODO: pretty print `.tpe`: https://github.com/scalameta/scalameta/issues/1479
          symbol.detail = denotation.symbol
          symbol.kind = kind
          symbol.range = model.resolveLocation(definition).range
          symbol.selectionRange = symbol.range
          symbol
      }
      js.Array[MonacoDocumentSymbol](symbols: _*)
    }
  }.toMonacoThenable

  def symbolKind(denotation: s.SymbolInformation): Option[SymbolKind] = {
    import denotation.kind._
    import s.SymbolInformation.Property
    def hasProperty(flag: Int): Boolean =
      (denotation.properties & flag) != 0

    if (isParameter || isTypeParameter)
      None
    else if (isField || hasProperty(Property.VAL.value | Property.VAR.value))
      Some(SymbolKind.Variable)
    else if (isMethod)
      Some(SymbolKind.Function)
    else if (isConstructor)
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
