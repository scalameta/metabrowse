package metadoc

import org.scalatest.FunSuite
import scala.collection.immutable.Seq
import scala.meta._
import metadoc.{schema => doc}

class IndexLookupTest extends FunSuite {
  val attrs = Attributes(
    dialect = dialects.Scala212,
    names = Seq(
      (
        Position.Range(Input.String("src/p/C.scala"), 1, 5),
        Symbol.Local("p.C")
      ),
      (
        Position.Range(Input.String("src/p/C.scala"), 6, 10),
        Symbol.Local("p.fun")
      ),
      (
        Position.Range(Input.String("src/p/C.scala"), 11, 14),
        Symbol.Local("lib.no")
      )
    ),
    messages = Seq.empty,
    denotations = Seq.empty,
    sugars = Seq.empty
  )

  val cDefinition = Some(doc.Position("src/p/C.scala", 1, 5))
  val cReferences = Map(
    "src/p/C.scala" ->
      doc.Ranges(
        List(
          doc.Range(20, 25),
          doc.Range(30, 35)
        )
      )
  )
  val cSymbol = doc.Symbol("p.C", cDefinition, cReferences)

  val funDefinition = Some(doc.Position("src/p/C.scala", 6, 10))
  val funReferences = Map(
    "src/p/C.scala" -> doc.Ranges(List(doc.Range(40, 45)))
  )
  val funSymbol = doc.Symbol("p.fun", funDefinition, funReferences)

  val noDefinition = None
  val noReferences = Map(
    "src/p/C.scala" -> doc.Ranges(List(doc.Range(37, 39)))
  )
  val noSymbol = doc.Symbol("lib.no", noDefinition, noReferences)

  val filename = "src/p/C.scala"
  val index = doc.Index(
    files = Seq(filename),
    symbols = Seq(cSymbol, funSymbol, noSymbol)
  )

  test("IndexLookup.findSymbol") {
    assert(IndexLookup.findSymbol(0, attrs, index) == None)
    assert(IndexLookup.findSymbol(1, attrs, index) == Some(cSymbol))
    assert(IndexLookup.findSymbol(5, attrs, index) == Some(cSymbol))
    assert(IndexLookup.findSymbol(6, attrs, index) == Some(funSymbol))
    assert(IndexLookup.findSymbol(11, attrs, index) == Some(noSymbol))
    assert(IndexLookup.findSymbol(14, attrs, index) == Some(noSymbol))
    assert(IndexLookup.findSymbol(15, attrs, index) == None)
  }

  test("IndexLookup.findDefinition") {
    assert(IndexLookup.findDefinition(3, attrs, index) == cDefinition)
    assert(IndexLookup.findDefinition(8, attrs, index) == funDefinition)
    assert(IndexLookup.findDefinition(0, attrs, index) == None)
  }

  def toPositions(map: Map[String, doc.Ranges]): Iterable[doc.Position] =
    map.flatMap {
      case (filename, ranges) =>
        ranges.ranges.map(
          range => doc.Position(filename, range.start, range.end)
        )
    }

  test("IndexLookup.findReferences") {
    import IndexLookup._
    val cReferencesPositions = toPositions(cReferences)
    val noReferencesPositions = toPositions(noReferences)
    assert(
      findReferences(3, true, attrs, index, filename) ==
        (cReferencesPositions ++ cDefinition)
    )
    assert(
      findReferences(3, false, attrs, index, filename) == cReferencesPositions
    )
    assert(
      findReferences(11, true, attrs, index, filename) == noReferencesPositions
    )
    assert(findReferences(0, true, attrs, index, filename).isEmpty)
    assert(findReferences(0, false, attrs, index, filename).isEmpty)
  }
}
