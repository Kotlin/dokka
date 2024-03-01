# Dokka UI test project

This is a Dokka test project for UI e2e tests.

The goal is to have as much variety of UI elements in one project as possible, so that during refactorings
we can compare the outputs between different versions of Dokka and make sure we didn't break any corner cases.

## How To

### Change Dokka version

The used Dokka version can be changed in [gradle/libs.versions.toml](gradle/libs.versions.toml).

Currently, this project works with release, `-dev`, `-test` and `-SNAPSHOT` versions.

### Run

```bash
./gradlew dokkaHtmlMultiModule
```
