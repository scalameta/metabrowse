package metabrowse.tests

import java.io.File
import java.util.concurrent.TimeUnit
import metabrowse.server.MetabrowseServer
import metabrowse.server.Sourcepath
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.selenium.Chrome
import scala.meta.interactive.InteractiveSemanticdb

class MetabrowseServerSuite
    extends FunSuite
    with Chrome
    with BeforeAndAfterAll {
  val server = new MetabrowseServer()
  val sourcepath = Sourcepath("org.typelevel:paiges-core_2.12:0.2.1")
  override def beforeAll(): Unit = {
    server.start(sourcepath)
  }
  override def afterAll(): Unit = {
    server.stop()
    quit()
  }

  val host = "http://localhost:4000/#"
  val DocScala = s"$host/org/typelevel/paiges/Doc.scala#L209C14-L209C19"
  val ChunkScala = s"$host/org/typelevel/paiges/Chunk.scala#L5C24"

  // See: https://github.com/SeleniumHQ/selenium/blob/master/rb/lib/selenium/webdriver/common/keys.rb
  val F12 = "\ue03C"
  val Command = "\ue03D"
  def sleep(seconds: Int): Unit = {
    Thread.sleep(TimeUnit.SECONDS.toMillis(seconds))
  }

  // NOTE(olafur): This is a first selenium test I ever write so it's quite hacky.
  // This test likely fails in a CI environment (needs chrome installed) and also fails
  // on non-macOS since it relies on the Mac-specific "Cmd" keyboard modifier.
  test("goto definition") {
    go to DocScala
    assert(pageTitle == "Metabrowse")
    className("mtk1")
    sleep(10)
    pressKeys(Command + F12)
    sleep(5)
    assert(currentUrl == ChunkScala)
    goBack()
    sleep(5)
    assert(currentUrl == DocScala)
  }

  test("urlForSymbol") {
    val g = InteractiveSemanticdb.newCompiler(
      sourcepath.classpath.mkString(File.pathSeparator),
      Nil
    )
    val some =
      g.rootMirror.staticClass("scala.Some").info.member(g.TermName("isEmpty"))
    val obtained = server.urlForSymbol(g)(some).get
    g.askShutdown()
    assert(obtained == "#/scala/Option.scala#L333C6")
  }
}
