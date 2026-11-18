#!/usr/bin/env bash
# Install the exact published Slimefun-Legacy API into Maven's local repository.
set -euo pipefail
VERSION="${SLIMEFUN_LEGACY_VERSION:-4.1.80}"
JAR="Slimefun-Legacy${VERSION}.jar"
URL="https://github.com/wickidcow/Slimefun-Legacy/releases/download/v${VERSION}/${JAR}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
curl --fail --location --retry 3 --silent --show-error "$URL" -o "$TMP/$JAR"
mvn -B org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file \
  -Dfile="$TMP/$JAR" \
  -DgroupId=com.github.wickidcow \
  -DartifactId=Slimefun-Legacy \
  -Dversion="$VERSION" \
  -Dpackaging=jar \
  -DgeneratePom=true
