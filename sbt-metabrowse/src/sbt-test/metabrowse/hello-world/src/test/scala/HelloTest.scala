package hello

import org.scalatest.funsuite.AnyFunSuite

class HelloTest extends AnyFunSuite {
  test("Hello.greeting") {
    assert(Hello.greeting(None) == "Hello, World!")
    assert(Hello.greeting(Some("Metabrowse User")) == "Hello, Metabrowse User!")
  }
}
