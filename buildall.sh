#!/usr/bin/env bash
set -eux

rm -rf out/

METADOC=0.1.1-3-dc394c6b-SNAPSHOT
CWD=$(pwd)

# Clone all repos
for repo in $(cat repos.txt); do
  git clone $repo || true
done


# Update all repos
for repo in $(cat repos.txt); do
  dir=$(echo $repo | xargs basename | sed "s/.git$//")
  echo $dir
  cd $dir
  git clean -xfd
  git pull origin master
  cp ../Semanticdb.scala project/
  sbt mbrowse
done

./gensite.sh $PWD/out


