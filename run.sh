#!/bin/bash
set -e && cd "${0%/*}"
./gradlew -q uberJar
#echo "exe: $exe"

java -ea -jar build/libs/rtmaven.uber.jar "$@"
#java -ea -jar "$exe" "$@"