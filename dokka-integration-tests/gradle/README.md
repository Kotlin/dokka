### Note
All Gradle projects inside the `project` subfolder can 
also be imported to the IDE by clicking on the corresponding
build.gradle.kts file -> "import gradle project". 

Before importing: Make sure that you have dokka installed
locally (`./gradlew publishToMavenLocal`).

To debug Gradle tests, the environment variable `ENABLE_DEBUG=true` should be declared.

### To update git submodules 

Integration tests have fixed git revision number, with the diff patch applied from the corresponding file (e.g. [`coroutines.diff`](projects/coroutines/coroutines.diff)).

In order to update:

* Checkout the project with the requered revision
    - It's some state of the `master`
* Manually write the diff (or apply the existing one and tweak) to have the project buildable against locally published Dokka of version `for-integration-tests-SNAPSHOT`
* `git diff > $pathToProjectInDokka/project.diff`
* Go to `$pathToProjectInDokka`, `git fetch && git checkout $revisionNumber`
    - Prior to that, ensure that you have your git submodules initialized
* Ensure that the corresponding `GradleIntegrationTest` passes locally and push


### Run integration tests with K2 (symbols)

To run integration tests with K2, the property `org.jetbrains.dokka.integration_test.useK2` should be set to `true`. 
By default, the task `integrationTest` is run with K1 (descriptors).
