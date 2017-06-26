package example

/** Docstring */
object Example {
  class Bar
  case class Foo[T <: Bar](e: T)
  def foo[T](e: T) = e
  foo(new Foo[Bar](new Bar).copy(e = new Bar))
  val x = 2
  def main(args: Array[String]): Unit = {
    println(x)
  }
}
