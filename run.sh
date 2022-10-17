#!/bin/bash
set -e && cd "${0%/*}"
./gradlew -q uberJar
echo

java -ea -jar build/libs/rtmaven.uber.jar "$@"