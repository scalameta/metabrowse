set -eux
OUT=$1
METADOC=0.1.1-4-be16a3a1-SNAPSHOT
# coursier launch org.scalameta:metadoc-cli_2.12:$METADOC \
#   -r sonatype:snapshots -- \
metadoc-nightly \
  --target $OUT \
  $(find . -name "classes" -type d) \
  $(find . -name "test-classes" -type d)
