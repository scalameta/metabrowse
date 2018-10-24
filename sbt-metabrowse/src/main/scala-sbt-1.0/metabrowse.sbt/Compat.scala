package metabrowse.sbt

import sbt._
import sbt.Keys._

object Compat {
  def forkOptions = Def.task {
    ForkOptions(
      javaHome = javaHome.value,
      outputStrategy = outputStrategy.value,
      bootJars = Vector.empty[java.io.File],
      workingDirectory = Option(baseDirectory.value),
      runJVMOptions = javaOptions.value.toVector,
      connectInput = connectInput.value,
      envVars = envVars.value
    )
  }
}
