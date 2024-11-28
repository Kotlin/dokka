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
   Note that the Gradle task will clone the repo into a `build/tmp` directory
   before copying it to a subdirectory inside `projects/`

3. Manually write the diff (or apply the existing one and tweak) to have the project buildable against locally published Dokka of version `for-integration-tests-SNAPSHOT`

   A git patch can be exported with:
   ```shell
   git diff > $pathToProjectInDokka/project.diff
   ```

4. Check that the corresponding `GradleIntegrationTest` passes locally and push

### Example projects

The [example Gradle projects for DGPv2](../examples/gradle-v2) are automatically tested.

The tests are located in [ExampleProjectsTest.kt](gradle/src/testExampleProjects/kotlin/ExampleProjectsTest.kt).
They validate that the example projects produce the expected HTML data, which is contained in the 
[gradle/src/testExampleProjects/expectedData](gradle/src/testExampleProjects/expectedData) directory.

#### Updating expected data

When the Dokka HTML output is updated, the tests will fail because the files in `expectedData`
will not match the actual generated HTML.

When a test fails it will log links to directories containing the actual and expected files
(in IntelliJ the links will be clickable).

To update the expected data:

1. Verify that the new data is valid, and does not contain error messages like "Error class: Unknown class".
2. Delete the 'expected' directory.
3. Copy the actual generated files to the same location.
4. Re-run the test to verify the tests pass.

### Run integration tests with K2 (symbols)

To run integration tests with K2, the property `org.jetbrains.dokka.integration_test.useK2` should be set to `true`.
By default, the task `integrationTest` is run with K1 (descriptors).
