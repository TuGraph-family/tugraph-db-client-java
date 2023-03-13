#!/bin/bash

# env config
export MAVEN_OPTS=-Xss10m

# build all
mvn clean -f pom.xml install -DskipTests