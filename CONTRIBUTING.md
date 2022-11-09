# Contributing Guidelines

There are three main ways you can contribute to Dokka's development:

* Submitting issues.
* Submitting fixes/changes/improvements via pull requests.
* Developing community plugins.

## Submitting issues

Bug reports, feature requests and questions are welcome. Submit issues [here](https://github.com/Kotlin/dokka/issues).

* Search for existing issues to avoid reporting duplicates.
* When submitting a bug report:
   * Test it against the most recently released version. It might have been fixed already.
   * Include code that reproduces the problem. Provide a complete reproducer, yet minimize it as much as 
     possible. A separate project that can be cloned is ideal.
   * If the bug is in behavior, explain what behavior you expected and what the actual result is.
* When submitting a feature request:
   * Explain why you need the feature.
   * Explaining the problem you face is more important than suggesting a solution.
     Report your problem even if you have no proposed solution.

## Submitting PRs

Dokka has extensive [Developer Guides](https://kotlin.github.io/dokka/1.7.20/developer_guide/introduction/) documentation
which goes over the development [Workflow](https://kotlin.github.io/dokka/1.7.20/developer_guide/workflow/) and 
[Dokka's architecture](https://kotlin.github.io/dokka/1.7.20/developer_guide/architecture/architecture_overview/),
which can help you understand how to achieve what you want and where to look.

All development (both new features and bugfixes) takes place in the `master` branch, it contains sources for the next
version of Dokka.

For any code changes:

* Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html).
  Use 4 spaces for indentation.
* [Build the project](#building) to make sure it all works and tests pass.
* Write tests that reproduce the bug or test new features.
* PRs without tests are accepted only in exceptional circumstances if it is evident that writing the
  corresponding test is too hard or otherwise impractical.
* If you add new or change old public API, [update public API dump](#updating-public-api-dump), otherwise it will fail 
  the build.

Please [contact maintainers](#contacting-maintainers) in advance to coordinate any big piece of work.

### Building

Building Dokka is pretty straightforward, with one small caveat: when you run `./gradlew build`, it will run integration
tests as well, which might take some time and will consume a lot of RAM (~20-30 GB), so you would usually want to exclude 
integration tests when building locally:

```Bash
./gradlew build -x integrationTest
```

Unit tests which are run as part of `build` should not take much time, but you can also skip it with `-x test`.

### Using/testing locally built Dokka

Below you will find a bare-bones instruction on how to use and test locally built Dokka. For more details and examples, 
visit [Workflow](https://kotlin.github.io/dokka/1.7.20/developer_guide/workflow/) topic.

1. Change `dokka_version` in `gradle.properties` to something that you will use later on as the dependency version.
   For instance, you can set it to something like `1.7.20-my-fix-SNAPSHOT`.
2. Publish it to Maven Local (`./gradlew publishToMavenLocal`)
3. In the project for which you want to generate documentation add Maven Local as a buildscript/dependency
   repository (`mavenLocal()`)
4. Update your Dokka dependency to the version you've just published:

```kotlin
plugins {
    id("org.jetbrains.dokka") version "1.7.20-my-fix-SNAPSHOT"
}
```

### Updating public API dump

[Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator/blob/master/README.md) 
is used to keep track of public API changes.

Run `./gradlew apiDump` to update API index files after introducing new or changing old public API. Commit updated 
API indexes together with other changes.

## Contacting maintainers

* If something cannot be done, not convenient, or does not work &mdash; submit an [issue](#submitting-issues).
* Discussions and general inquiries &mdash; use `#dokka` channel in 
  [Kotlin Community Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up).
