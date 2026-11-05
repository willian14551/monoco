#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
printf '%s\n' 'Modo H2: http://localhost:8080/api/. Para MySQL, use o Docker.'
exec sh ./gradlew --console=plain bootRun --args='--spring.profiles.active=local'
