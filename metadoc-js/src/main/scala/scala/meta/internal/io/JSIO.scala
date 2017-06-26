package scala.meta.internal.io

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/*
 * XXX: Ugly workaround to avoid 'Module not found' errors.
 *
 * Scalameta's Scala.js support depends on several Node.js modules which
 * are not supported nor needed in the browser.
 */

@js.native
@JSGlobal
object JSShell extends js.Any

@js.native
@JSGlobal
object JSFs extends js.Any

@js.native
@JSGlobal
class JSStats extends js.Any

@js.native
@JSGlobal
object JSPath extends js.Any
