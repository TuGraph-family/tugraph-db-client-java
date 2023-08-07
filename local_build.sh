#!/bin/bash

# env config
export MAVEN_OPTS=-Xss10m

# build all
mvn --batch-mode --update-snapshots --no-transfer-progress clean -f pom.xml package -DskipTests -Dmaven.javadoc.skip -Dorg.slf4j.simpleLogger.defaultLogLevel=WARN
