#!/bin/bash

# env config
export MAVEN_OPTS=-Xss10m

# build all
mvn --batch-mode --update-snapshots --no-transfer-progress clean -f pom.xml install -DskipTests -Dmaven.javadoc.skip