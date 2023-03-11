#!/usr/bin/env bash

export MAVEN_OPTS=-Xss10m

# Add huawei mirror for parent repos
mkdir -p ~/.m2
echo "<settings>
  <mirrors>
    <mirror>
      <id>huaweicloud</id>
      <mirrorOf>*</mirrorOf>
      <url>https://repo.huaweicloud.com/repository/maven/</url>
    </mirror>
  </mirrors>
</settings>" > ~/.m2/settings.xml

mvn clean install -DskipTests

