# Dokka UI test project

This is a Dokka test project for UI e2e tests.

The goal is to have as much variety of UI elements in one project as possible, so that during refactorings
we can compare the outputs between different versions of Dokka and make sure we didn't break any corner cases.

### Run from root of the project

```bash
export DOKKA_TEST_OUTPUT_PATH="build/ui-showcase-result"
./gradlew :dokka-integration-tests:gradle:testUiShowcaseProject
```
