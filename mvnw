#!/bin/sh
# Maven Wrapper Script for Unix/macOS
# ---------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information.
# ---------------------------------------------------------------------------

set -e

MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"

# Determine the project root (where this script lives)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Download Maven if not already cached
if [ -f "$MAVEN_WRAPPER_JAR" ]; then
  echo "Maven wrapper jar found."
else
  echo "Downloading Maven Wrapper..."
  WRAPPER_URL=$(grep wrapperUrl "$MAVEN_WRAPPER_PROPERTIES" | cut -d= -f2)
  curl -o "$MAVEN_WRAPPER_JAR" -L "$WRAPPER_URL"
fi

# Determine cached Maven installation
DISTRIBUTION_URL=$(grep distributionUrl "$MAVEN_WRAPPER_PROPERTIES" | cut -d= -f2)
MAVEN_HOME="$HOME/.m2/wrapper/dists"

exec "$JAVA_HOME/bin/java" \
  -classpath "$MAVEN_WRAPPER_JAR" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
