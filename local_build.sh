#!/bin/bash

# env config
export MAVEN_OPTS=-Xss10m
mkdir -p ~/.m2
echo "<settings>
  <mirrors>
    <mirror>
      <id>alimaven</id>
      <mirrorOf>central</mirrorOf>
      <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
    </mirror>
  </mirrors>
</settings>" > ~/.m2/settings.xml

# build all
mvn clean -f pom.xml install -DskipTests