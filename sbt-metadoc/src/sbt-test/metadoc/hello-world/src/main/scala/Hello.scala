package hello

object Hello {
  def main(args: Array[String]): Unit = {
    val hello = greeting(args.headOption)
    println(hello)
  }

  def greeting(name: Option[String]): String =
    s"Hello, ${name.getOrElse("World")}!"
}
