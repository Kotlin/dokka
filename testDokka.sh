#!/bin/bash

#
# Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
#

# New version to be published
NEW_VERSION="1.9.20-my-fix-SNAPSHOT"

# Path to test project
TEST_PROJECT_PATH="./examples/gradle/dokka-gradle-example"

# Path to Dokka
DOKKA_REPO_PATH="./"

# Port to view results
PORT=8001

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
        -p|--port)
            PORT="$2"
            shift 2
            ;;
        *)
            echo "Wrong parameter: $1"
            exit 1
            ;;
    esac
done

echo "Test locally Dokka version $NEW_VERSION"
echo "Test project path: $TEST_PROJECT_PATH"
echo "Dokka path: $DOKKA_REPO_PATH"
echo "Port: $PORT"

# 1. Publish to local Maven repository
cd "$DOKKA_REPO_PATH"
./gradlew publishToMavenLocal -Pversion=$NEW_VERSION

# 2. Update Dokka version in test project
cd "$TEST_PROJECT_PATH"
sed -i "" "s/\(id(\"org\.jetbrains\.dokka\") version\) \".*\"/\1 \"$NEW_VERSION\"/"  build.gradle.kts

# 3. Build and generate documentation
./gradlew clean && ./gradlew dokkaHTML

wait

# 4 Vacate port
# Find PID of process listening on port
PID=$(lsof -t -i :$PORT)

# Check that PID is not empty
if [ -n "$PID" ]; then
    echo "Kill process with PID $PID"
    kill -9 "$PID"
else
    echo "Port $PORT is free"
fi

# 5.1 Echo link to documentation
echo "Open http://localhost:$PORT in browser"


# 5.2 Start Python server to view results
cd "./build/dokka/html"

echo 'Start Python server in directory'
echo "$TEST_PROJECT_PATH/build/dokka/html"

python3 -m http.server $PORT
