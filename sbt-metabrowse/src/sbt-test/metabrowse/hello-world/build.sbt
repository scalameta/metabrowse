name := "test"
enablePlugins(MetabrowsePlugin)
scalaVersion := _root_.metabrowse.sbt.BuildInfo.scalaVersion
metabrowseSettings // enable semanticdb-scalac
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0" % Test

def assertExists(file: File, message: String) =
  assert(file.exists, message)

TaskKey[Unit]("check") := {
  val dir = metabrowse.value

  // Test that sources for both main and test configurations are handled.
  val expectedSemanticDbs = List(
    "src/main/scala/Hello.scala.semanticdb.gz",
    "src/test/scala/HelloTest.scala.semanticdb.gz"
  )

  assertExists(dir, s"Metabrowse output directory does not exist: $dir")

  for (semanticDb <- expectedSemanticDbs)
    assertExists(
      dir / "semanticdb" / semanticDb,
      s"Semantic DB does not exist: $semanticDb"
    )

}
