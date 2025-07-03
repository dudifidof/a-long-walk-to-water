#!/usr/bin/env bash
# Simple helper to build the mod and launch the client
set -e
cd "$(dirname "$0")"
./gradlew clean build
./gradlew runClient
