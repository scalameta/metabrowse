#!/bin/bash

# Bash strict mode
set -euo pipefail
ORIG_IFS="$IFS"
IFS=$'\n\t'

set -x

ROOT_DIR="$(git rev-parse --show-toplevel)"
TS_IMPORTER_DIR="$ROOT_DIR/project/ts-importer"
NODE_MODULES_DIR="$ROOT_DIR/metadoc-js/target/scala-2.12/scalajs-bundler/main/node_modules"
NPM_MONACO_D_TS="$ROOT_DIR/metadoc-js/target/scala-2.12/scalajs-bundler/main/node_modules/monaco-editor/monaco.d.ts"
MONACO_D_TS="$TS_IMPORTER_DIR/monaco.d.ts"
MONACO_SCALA="$ROOT_DIR/metadoc-js/src/main/scala/monaco/Monaco.scala"

if [[ ! -e "$NPM_MONACO_D_TS" ]]; then
  (cd "$ROOT_DIR" && sbt -batch clean js/compile:npmUpdate)
fi

if [[ ! -d "$TS_IMPORTER_DIR" ]]; then
  git clone https://github.com/sjrd/scala-js-ts-importer.git "$TS_IMPORTER_DIR"
fi

# Curated list of edits required for scala-js-ts-importer to parse monaco.d.ts
sed \
   -e 's/): [^ ]* is [^; ]*;/): boolean;/' \
   -e 's/BuiltinTheme = .*;/BuiltinTheme = string;/' \
   -e 's/: void | /: /' \
   -e 's/onDidChange?: IEvent<this>/onDidChange?: IEvent<CodeLensProvider>/' \
   -e "s/\([a-zA-Z]\+[?]\?\): '[a-zA-Z0-9| '-]*'\([,;]\?\)$/\1: string\2/" \
   -e 's/lineNumbers?: .*;/lineNumbers?: string/' \
   -e '/^declare module monaco.languages.\(typescript\|html\|css\|json\) {$/,/^}$/d' \
   < "$NPM_MONACO_D_TS" \
   > "$MONACO_D_TS"

(cd "$TS_IMPORTER_DIR" && sbt -batch "run $MONACO_D_TS $MONACO_SCALA")
