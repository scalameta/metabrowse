# metadoc

Experimenting with using Scalameta Semantic DB to build online code browser
with "jump to definition" and "see usage". To run,

```
git clone ...
cd metadoc
sbt ~metadocJVM/test
npm install -g browser-sync
browser-sync start --server --files "metadoc/jvm/target/metadoc/**"
open http://localhost:3000/metadoc/jvm/target/metadoc/example/src/main/scala/example/Example.scala.html
```

