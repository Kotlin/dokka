# Dokka UI test project

This is a Dokka test project for UI e2e tests.

The goal is to have as much variety of UI elements in one project as possible, so that during refactorings
we can compare the outputs between different versions of Dokka and make sure we didn't break any corner cases.

### Run from the root of the project

```bash
export DOKKA_TEST_OUTPUT_PATH="build/ui-showcase-result"
./gradlew :dokka-integration-tests:gradle:testUiShowcaseProject
```

### Run with the published Dokka version

Dokka should be published in one of the following repositories:
[MavenCentral](https://central.sonatype.com),
[dev](https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev) or
[test](https://maven.pkg.jetbrains.space/kotlin/p/dokka/test)

```bash
export DOKKA_TEST_OUTPUT_PATH="build/ui-showcase-result"
./gradlew :dokka-integration-tests:gradle:testUiShowcaseProject -Porg.jetbrains.dokka.integration_test.dokkaVersionOverride=2.0.0-dev-329
```
