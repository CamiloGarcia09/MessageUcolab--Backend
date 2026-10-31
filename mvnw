#!/bin/sh
# Reuse the module launcher with the root reactor's wrapper configuration.
MAVEN_BASEDIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd) || exit 1
export MAVEN_BASEDIR
exec sh "$MAVEN_BASEDIR/core/mvnw" "$@"
