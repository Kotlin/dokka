# Dokka Integration Tests

### Gradle projects

All Gradle projects inside the `project` subfolder can
also be imported to the IDE by clicking on the corresponding
build.gradle.kts file -> "import gradle project".

Before importing: Make sure that you have dokka installed
locally (`./gradlew publishToMavenLocal`).

To debug Gradle tests, the environment variable `ENABLE_DEBUG=true` should be declared.

### External Projects

Some Maven and Gradle integration tests use external projects for testing.

Each project is automatically checked out (using JGit) and updated with a Gradle task.

#### Updating external projects

The external projects need to be modified before they can be tested to use the latest version of Dokka, or enable
specific Dokka features. The projects are modified using a git patch
(e.g. [`coroutines.diff`](gradle/projects/coroutines/coroutines.diff)).
The Integration Test will apply the git patch on demand.

Here's how to update an external project:

1. Find the git commit sha for the required version of the external project

2. Update the `commitId` in the Gradle task
    ```kotlin
    val checkoutKotlinxSerialization by tasks.registering(GitCheckoutTask::class) {
        uri = "https://github.com/Kotlin/kotlinx.serialization.git"
        commitId = "<new-commit-sha-here>"
        destination = templateProjectsDir.dir("serialization/kotlinx-serialization")
    }
    ```
3. Run the Gradle task

   ```shell
   ./gradlew :dokka-integration-tests:gradle:checkoutKotlinxSerialization
   ```

4. The `GitCheckoutTask` task will first clone the repo into a `build/tmp` directory.
   Open this directory in an IDE, and edit the project to be testable.

   ```shell
   # open with IntelliJ IDEA
   idea ./gradle/build/tmp/checkoutKotlinxSerialization/repo
   ```

   - Remove `mavenLocal()` repositories.
   - Add `/* %{DOKKA_IT_MAVEN_REPO}% */` to the top of `repositories {}` blocks.
   - Update Dokka version.
     ```diff
     - classpath "org.jetbrains.dokka:dokka-gradle-plugin:$dokka_version"
     + classpath "org.jetbrains.dokka:dokka-gradle-plugin:${providers.gradleProperty("dokka_it_dokka_version").get()}"
     ```

   ⚠️ `GitCheckoutTask` will delete any changes, so make sure not to run it again while you're editing.
5. Once you're happy, export a git patch.
   ```shell
   cd ./gradle/build/tmp/checkoutKotlinxSerialization/repo;
   git diff > updated.diff
   ```
   Update the patch in the [projects](./gradle/projects/) directory,
   e.g. [`dokka-integration-tests/gradle/projects/serialization/serialization.diff`](./gradle/projects/serialization/serialization.diff).
6. Run the corresponding test task.
   ```shell
   ./gradlew :dokka-integration-tests:gradle:testExternalProjectKotlinxSerialization
   ```
7. Once the test works, commit and push.

### Run integration tests with K2 (symbols)

To run integration tests with K2, the property `org.jetbrains.dokka.integration_test.useK2` should be set to `true`.
By default, the task `integrationTest` is run with K1 (descriptors).
