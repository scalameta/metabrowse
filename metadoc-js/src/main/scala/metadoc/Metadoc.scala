package metadoc

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.TypedArrayBuffer
import org.scalajs.dom
import scala.meta._
import metadoc.schema.Index

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object MetadocApp extends js.JSApp {
  def main(): Unit = {
    for {
      monaco <- Monaco.load()
      indexBytes <- fetchBytes("metadoc.index")
      Index(files, _) = Index.parseFrom(indexBytes)
      bytes <- fetchBytes("semanticdb/" + files(0).replace(".scala", ".semanticdb"))
    } {
      Database.load(bytes).entries.collectFirst {
        case (Input.LabeledString(fileName, contents), _) =>
          openEditor(monaco, fileName, contents)
      }
    }
  }

  def openEditor(monaco: Monaco, fileName: String, contents: String): Unit = {
    val app = dom.document.getElementById("editor")
    app.innerHTML = ""
    monaco.languages.register(ScalaLanguageExtensionPoint)
    monaco.languages.setMonarchTokensProvider(ScalaLanguageExtensionPoint.id, ScalaLanguage.language)
    monaco.languages.setLanguageConfiguration(ScalaLanguageExtensionPoint.id, ScalaLanguage.conf)
    dom.document.getElementById("title").textContent = fileName

    val editor = monaco.editor.create(app, new MonacoEditor.IEditorConstructionOptions {
      override val readOnly = true
      override val value = contents
      override val language = "scala"
    })

    dom.window.onresize = { _: dom.UIEvent => editor.layout() }
  }

  def fetchBytes(url: String): Future[Array[Byte]] = {
    for {
      response <- dom.experimental.Fetch.fetch(url).toFuture
      if response.status == 200
      buffer <- response.arrayBuffer().toFuture
    } yield {
      val bytes = Array.ofDim[Byte](buffer.byteLength)
      TypedArrayBuffer.wrap(buffer).get(bytes)
      bytes
    }
  }

}
