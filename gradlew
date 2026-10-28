#!/bin/sh
set -eu
cd "$(dirname "$0")"
exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
