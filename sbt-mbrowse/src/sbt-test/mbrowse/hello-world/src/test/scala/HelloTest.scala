package hello

import org.scalatest.FunSuite

class HelloTest extends FunSuite {
  test("Hello.greeting") {
    assert(Hello.greeting(None) == "Hello, World!")
    assert(Hello.greeting(Some("Mbrowse User")) == "Hello, Mbrowse User!")
  }
}
