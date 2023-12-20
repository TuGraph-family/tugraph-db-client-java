#!/bin/bash

# env config
export MAVEN_OPTS=-Xss10m
mkdir -p ~/.m2
echo "<settings>
  <mirrors>
    <mirror>
      <id>alimaven</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/nexus/content/groups/public/</url>
    </mirror>
  </mirrors>
</settings>" > ~/.m2/settings.xml

# build all
mvn --batch-mode --update-snapshots --no-transfer-progress clean -f pom.xml package -DskipTests -Dmaven.javadoc.skip -Dorg.slf4j.simpleLogger.defaultLogLevel=WARN
