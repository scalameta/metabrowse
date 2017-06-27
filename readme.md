# metadoc

[![Build Status](https://travis-ci.org/olafurpg/metadoc.svg?branch=master)](https://travis-ci.org/olafurpg/metadoc)

An experiment with [Scalameta Semantic API](http://scalameta.org/tutorial/#SemanticAPI)
to build online code browser with "jump to definition" and "see usage". To run,

```
git clone https://github.com/scalameta/metadoc.git
cd metadoc
npm install -g yarn
sbt metadoc-site js/fastOptJS::startWebpackDevServer
open http://localhost:8080
```
