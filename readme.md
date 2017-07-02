# Metadoc

[![Build Status](https://travis-ci.org/scalameta/metadoc.svg?branch=master)](https://travis-ci.org/scalameta/metadoc)
[![Join the chat at https://gitter.im/scalameta/scalameta](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scalameta/metadoc)

An experiment with [Scalameta Semantic API](http://scalameta.org/tutorial/#SemanticAPI)
to build online code browser with "jump to definition" and "see usage". To run,

```
git clone https://github.com/scalameta/metadoc.git
cd metadoc
npm install -g yarn
sbt
  > metadoc-site                        # generate static site under target/metadoc.
  > js/fastOptJS::startWebpackDevServer # spin up local file server that listens for changes.
open http://localhost:8080
```

### Implemented features

- Generates a static site that is possible to serve via GitHub pages
- Jump to definition
- Find usages
- Search by symbol

For other features on the roadmap, see our issue tracker: https://github.com/scalameta/metadoc/issues

### Related projects

- sxr: https://github.com/sbt/sxr/

### Team

The current maintainers (people who can merge pull requests) are:

* Eugene Burmako - [`@xeno-by`](https://github.com/xeno-by)
* Jonas Fonseca - [`@jonas`](https://github.com/jonas)
* Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

An up-to-date list of contributors is available here: https://github.com/scalameta/metadoc/graphs/contributors.

