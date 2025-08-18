#!/usr/bin/env sh
# Minimal Gradle wrapper script for Linux/macOS (CI-safe)

set -e

# Resolve APP_HOME (directory of this script)
PRG="$0"
while [ -h "$PRG" ]; do
  ls="$(ls -ld "$PRG")"
  link="$(expr "$ls" : '.*-> \(.*\)$')"
  if expr "$link" : '/.*' >/dev/null; then
    PRG="$link"
  else
    PRG="$(dirname "$PRG")/$link"
  fi
done
SAVED="$(pwd)"
cd "$(dirname "$PRG")" >/dev/null
APP_HOME="$(pwd -P)"
cd "$SAVED" >/dev/null

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
MAINCLASS=org.gradle.wrapper.GradleWrapperMain
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Pick Java
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1; then
  echo "ERROR: Java not found. Set JAVA_HOME or add 'java' to PATH." >&2
  exit 1
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS \
  -Dorg.gradle.appname=gradlew \
  -classpath "$CLASSPATH" $MAINCLASS "$@"
