package mbrowse.sbt

import sbt._
import sbt.Keys._

object Compat {
  def forkOptions = Def.task {
    ForkOptions(
      bootJars = Nil,
      javaHome = javaHome.value,
      connectInput = connectInput.value,
      outputStrategy = outputStrategy.value,
      runJVMOptions = javaOptions.value,
      workingDirectory = Some(baseDirectory.value),
      envVars = envVars.value
    )
  }
}
