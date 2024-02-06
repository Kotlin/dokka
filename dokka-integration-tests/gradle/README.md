### Note
All Gradle projects inside the `project` subfolder can 
also be imported to the IDE by clicking on the corresponding
build.gradle.kts file -> "import gradle project". 

Before importing: Make sure that you have dokka installed
locally (`./gradlew publishToMavenLocal`).

To debug Gradle tests, the environment variable `ENABLE_DEBUG=true` should be declared.

### Run integration tests with K2 (symbols)

To run integration tests with K2, the property `org.jetbrains.dokka.integration_test.useK2` should be set to `true`. 
By default, the task `integrationTest` is run with K1 (descriptors).
