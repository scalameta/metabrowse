name := "test"
enablePlugins(MetadocPlugin)
scalaVersion := _root_.metadoc.sbt.BuildInfo.scalaVersion
metadocSettings // enable semanticdb-scalac
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % Test

def assertExists(file: File, message: String) =
  assert(file.exists, message)

TaskKey[Unit]("check") := {
  val dir = metadoc.value

  // Test that sources for both main and test configurations are handled.
  val expectedSemanticDbs = List(
    "src/main/scala/Hello.scala.semanticdb.gz",
    "src/test/scala/HelloTest.scala.semanticdb.gz"
  )

  assertExists(dir, s"Metadoc output directory does not exist: $dir")

  for (semanticDb <- expectedSemanticDbs)
    assertExists(
      dir / "semanticdb" / semanticDb,
      s"Semantic DB does not exist: $semanticDb"
    )

}
