name := "test"
enablePlugins(MetadocPlugin)
scalaVersion := "2.11.11"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % Test

// Copied from the CLI code
def encodeSymbolName(name: String): String = {
  val md = java.security.MessageDigest.getInstance("SHA-512")
  val sha = md.digest(name.getBytes("UTF-8"))
  // 512 bits ~> 64 bytes and doubled for the hex encoding
  String.format("%0128x", new java.math.BigInteger(1, sha))
}

def assertExists(file: File, message: String) =
  assert(file.exists, message)

TaskKey[Unit]("check") := {
  val dir = metadoc.value
  val expectedSemanticDbs = List(
    "src/main/scala/Hello.semanticdb",
    "src/test/scala/HelloTest.semanticdb"
  )
  val expectedSymbols = List(
    "_root_.hello.Hello.greeting(Lscala/Option;)Ljava/lang/String;.",
    "_root_.hello.Hello.",
    "_root_.hello.Hello.main([Ljava/lang/String;)V.",
    "_root_.hello.HelloTest#",
    "_root_.hello.Hello.greeting(Lscala/Option;)Ljava/lang/String;.(name)",
    "_root_.hello.",
    "src/main/scala/Hello.scala@76..113",
    "_root_.hello.Hello.main([Ljava/lang/String;)V.(args)"
  )

  assertExists(dir, s"Metadoc output directory does not exist: $dir")
  assertExists(dir / "metadoc.index", s"Metadoc index file does not exist")

  for (semanticDb <- expectedSemanticDbs)
    assertExists(
      dir / "semanticdb" / semanticDb,
      s"Semantic DB does not exist: $semanticDb"
    )

  for (symbol <- expectedSymbols)
    assertExists(
      dir / "symbol" / encodeSymbolName(symbol),
      s"Symbol does not exist: $symbol"
    )

  val actualSymbols = (dir / "symbol").listFiles().length
  assert(
    actualSymbols == expectedSymbols.length,
    s"Found $actualSymbols symbols, expected ${expectedSymbols.length}"
  )
}
