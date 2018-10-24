package hello

import org.scalatest.FunSuite

class HelloTest extends FunSuite {
  test("Hello.greeting") {
    assert(Hello.greeting(None) == "Hello, World!")
    assert(Hello.greeting(Some("Metabrowse User")) == "Hello, Metabrowse User!")
  }
}
