# Metadoc

[![Build Status](https://travis-ci.org/scalameta/metadoc.svg?branch=master)](https://travis-ci.org/scalameta/metadoc)
[![Join the chat at https://gitter.im/scalameta/scalameta](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scalameta/metadoc)

An experiment with [Scalameta Semantic API](http://scalameta.org/tutorial/#SemanticAPI)
to build online code browser with IDE-like features such as "jump to definition" and "find usages".
See our [demo](http://scalameta.org/metadoc/) page to try it out!

## Implemented features

- Generates a static site that is possible to serve via GitHub pages
- Jump to definition
- Find usages
- Search by symbol

For other features on the roadmap, see our issue tracker: https://github.com/scalameta/metadoc/issues

## Related projects

- sxr: https://github.com/sbt/sxr/

## Contributing

To run metadoc locally,

```
git clone https://github.com/scalameta/metadoc.git
cd metadoc
git submodule init
git submodule update
npm install -g yarn
sbt
  > metadoc-site                        # generate static site under target/metadoc.
  > js/fastOptJS::startWebpackDevServer # spin up local file server that listens for changes.
  > ~js/fastOptJS                       # compiles Scala.js application, browser refreshes on edit.
open http://localhost:8080
```

### Upgrading the Monaco Editor

Metadoc interfaces with the Monaco Editor uses a Scala.js facade based on the
`monaco.d.ts` TypeScript type definition file provided as part of the
`monaco-editor` NPM package. The facade can be generated with
[scala-js-ts-importer], however, manual merging is necessary, since the facade
contains several tweaks.

The following instructions gives a .

 - Update the Monaco Editor version in `build.sbt`
   ```scala
   npmDependencies in Compile ++= Seq(
     "monaco-editor" -> "x.y.z",
     // ...
   )
   ```
 - Update the NPM packages after cleaning to force download of the new version
   ```
   $ sbt -batch clean js/compile:npmUpdate
   ```
 - Run script to update the `Monaco.scala` file
   ```
   $ bin/update-monaco-facade.sh
   ```
   This might require tweaking the curated list of edits (the `sed` command in
   the script.
 - Fix the `package importedjs {}` code inserted by [scala-js-ts-importer] by
   manually editing `Monaco.scala`
 - Reformat `Monaco.scala`
   ```
   $ bin/scalafmt metadoc-js/src/main/scala/monaco/Monaco.scala
   ```

At this point merge the changes to `Monaco.scala` using `git add -i` or some
other tool, like `tig`. Stage any newly introduced types or methods and revert
changes that remove tweaks, such as use of `override` in front of `clone` and
`toString` methods.

 [scala-js-ts-importer]: https://github.com/sjrd/scala-js-ts-importer

## Team

The current maintainers (people who can merge pull requests) are:

* Eugene Burmako - [`@xeno-by`](https://github.com/xeno-by)
* Jonas Fonseca - [`@jonas`](https://github.com/jonas)
* Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

An up-to-date list of contributors is available here: https://github.com/scalameta/metadoc/graphs/contributors.
