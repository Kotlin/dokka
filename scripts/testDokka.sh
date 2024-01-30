#!/bin/bash
#
# Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
#
set -e

# Get the path to the script itself
SCRIPT_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Path to Dokka
DOKKA_REPO_PATH="$SCRIPT_PATH/../"

# Path to test project
TEST_PROJECT_PATH="./examples/gradle/dokka-gradle-example"

# New version to be published
NEW_VERSION="1.9.20-my-fix-SNAPSHOT"

# Port to view results
PORT=8001

#
IS_MULTIMODULE=0

# 0. Parse command line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        -v|--version)
            NEW_VERSION="$2"
            shift 2
            ;;
          -d|--directory)
            TEST_PROJECT_PATH="$2"
            shift 2
            ;;
          -m|--multimodule)
            IS_MULTIMODULE=1
            shift
            ;;
        -p|--port)
            PORT="$2"
            shift 2
            ;;
        *)
            echo "TestDokka: Wrong parameter: $1"
            exit 1
            ;;
    esac
done

echo "TestDokka: Test locally Dokka version $NEW_VERSION"
echo "TestDokka: Test project path: $TEST_PROJECT_PATH"
echo "TestDokka: Is multimodule: $IS_MULTIMODULE"
echo "TestDokka: Dokka path: $DOKKA_REPO_PATH"
echo "TestDokka: Port: $PORT"

# 1. Publish to local Maven repository
cd "$DOKKA_REPO_PATH"
echo "TestDokka: Publish to local Maven repository"
./gradlew publishToMavenLocal -Pversion="$NEW_VERSION"

# 2. Update Dokka version in test project
cd "$TEST_PROJECT_PATH"
if [ -f "build.gradle.kts" ]; then
    echo "TestDokka: Update version in build.gradle.kts"
    sed -i "" "s/\(id(\"org\.jetbrains\.dokka\") version\) \".*\"/\1 \"$NEW_VERSION\"/"  build.gradle.kts
fi

if [ -f "gradle.properties" ]; then
     echo "TestDokka: Update version in gradle.properties"
    sed -i "" "s/dokka_version=.*/dokka_version=$NEW_VERSION/"  gradle.properties
fi


# 3. Build and generate documentation
if [ "$IS_MULTIMODULE" -eq 1 ]; then
    echo "TestDokka: Build multimodule project"
    ./gradlew clean && ./gradlew dokkaHtmlMultiModule
else
    echo "TestDokka: Build single module project"
    ./gradlew clean && ./gradlew dokkaHTML
fi

wait

# 4 Vacate port
# Find PID of process listening on port
PID=$(lsof -t -i :"$PORT" || true)

# Check that PID is not empty
if [ -n "$PID" ]; then
    echo "TestDokka: Kill process with PID $PID"
    kill -9 "$PID"
else
    echo "TestDokka: Port $PORT is free"
fi

# 5.1 Echo link to documentation
echo "TestDokka: Open http://localhost:$PORT in browser"

# 5.2 Start Python server to view results
if [ "$IS_MULTIMODULE" -eq 1 ]; then
    cd "./build/dokka/htmlMultiModule"
else
    cd "./build/dokka/html"
fi

echo 'TestDokka: Start Python server in directory'
echo "TestDokka: $TEST_PROJECT_PATH/build/dokka/html"

python3 -m http.server "$PORT"

echo "TestDokka: Done"
