#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
exec ./gradlew bootRun --args='--spring.profiles.active=local'
