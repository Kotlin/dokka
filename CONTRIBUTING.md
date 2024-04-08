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

Dokka has extensive [Developer Guides](https://kotlin.github.io/dokka/1.9.20/developer_guide/introduction/) documentation
which goes over the development [Workflow](https://kotlin.github.io/dokka/1.9.20/developer_guide/workflow/) and 
[Dokka's architecture](https://kotlin.github.io/dokka/1.9.20/developer_guide/architecture/architecture_overview/),
which can help you understand how to achieve what you want and where to look.

All development (both new features and bugfixes) takes place in the `master` branch, it contains sources for the next
version of Dokka.

For any code changes:

* Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html).
  Use 4 spaces for indentation.
* [Build the project](#build-dokka-locally) to make sure it all works and tests pass.
* Write tests that reproduce the bug or test new features.
* PRs without tests are accepted only in exceptional circumstances if it is evident that writing the
  corresponding test is too hard or otherwise impractical.
* If you add new or change old public API, [update public API dump](#updating-public-api-dump), otherwise it will fail 
  the build.

Please [contact maintainers](#contacting-maintainers) in advance to coordinate any big piece of work.

## Working with the code

### Build Dokka locally

Building Dokka is pretty straightforward:

```Bash
./gradlew build
```

This will build all subprojects and trigger `build` in all composite builds. If you are working on a 
[runner](dokka-runners), you can build it independently.

Checks that are performed as part of `build` do not require any special configuration or environment, and should 
not take much time (~2-5 minutes), so please make sure they pass before submitting a pull request.

### Use/test locally built Dokka

Below you will find a bare-bones instruction on how to use and test locally built Dokka. For more details and examples, 
visit [Workflow](https://kotlin.github.io/dokka/1.9.20/developer_guide/workflow/) topic.

1. Publish a custom version of Dokka to Maven Local: `./gradlew publishToMavenLocal -Pversion=1.9.20-my-fix-SNAPSHOT`
2. In the project for which you want to generate documentation add Maven Local as a buildscript/dependency
   repository (`mavenLocal()`)
3. Update your Dokka dependency to the version you've just published:

```kotlin
plugins {
    id("org.jetbrains.dokka") version "1.9.20-my-fix-SNAPSHOT"
}
```

There is an automation script for this routine, see [testDokka.sh.md](scripts/testDokka.sh.md) for details.

### Updating public API dump

[Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator/blob/master/README.md) 
is used to keep track of public API changes.

Run `./gradlew apiDump` to update API index files after introducing new or changing old public API. Commit updated 
API indexes together with other changes.

### Run integration tests

Dokka's [integration tests](dokka-integration-tests) help check compatibility with various versions of Kotlin, Android, 
Gradle and Java. They apply Dokka to real user-like projects and invoke Gradle / Maven / CLI tasks to generate the 
documentation.

Integration tests require a significant amount of available RAM (~20-30GB), take 1+ hour and may require additional 
environment configuration to run. For these reasons, it's not expected that you run all integration tests locally 
as part of the everyday development process, they will be run on CI once you submit a PR.

However, if you need to run all integration tests locally, you can use the `integrationTest` task:

```bash
./gradlew integrationTest
```

If you need to run a specific test locally, you can run it from your IDE or by calling the corresponding Gradle
task (for example, `:dokka-integration-tests:gradle:testExternalProjectKotlinxCoroutines`).

## Infrastructure

### Java version

To minimize compatibility problems, [Gradle's Java toolchains](https://docs.gradle.org/current/userguide/toolchains.html)
are [used](build-logic/src/main/kotlin/org/jetbrains/conventions/base-java.gradle.kts) to build and test the project.

When run, Gradle tries to auto-detect the required JRE/JDK installation locally, but it may fail if you don't have
that version of Java installed or if it's installed in an unusual location. Please refer to 
[Gradle's documentation on toolchains](https://docs.gradle.org/current/userguide/toolchains.html#sec:auto_detection)
for customization and problem resolution questions.

You can use the following [Gradle properties](gradle.properties) to build/test
Dokka with a different version of Java:

| Property                                         | Default value | Description                                      |
|--------------------------------------------------|---------------|--------------------------------------------------|
| `org.jetbrains.dokka.javaToolchain.mainCompiler` | `8`           | The version used to build Dokka projects.        |
| `org.jetbrains.dokka.javaToolchain.testLauncher` | `8`           | The version used run unit and integration tests. |

Separating the compiler and test versions is needed to check Dokka's compatibility with various Java versions.
For example, the GitHub Actions based unit tests are 
[run under multiple versions of Java](.github/workflows/tests-thorough.yml)
at the same time.

### GitHub Actions

The majority of automated checks and builds are run as 
[GitHub Actions workflows](https://docs.github.com/en/actions/using-workflows/about-workflows).

The configuration for Dokka's workflows can be found in [`.github/workflows`](.github/workflows). 

For your first PR, a maintainer will need to approve the workflow runs manually. For your subsequent PRs, the workflows
will be triggered and run automatically on every PR commit.

While GitHub Actions checks can expose real problems, [TeamCity-based integration tests](#teamcity) need to be run for 
any significant changes as they have more thorough compatibility checks. 

Notable workflows:

* [Publish preview to GitHub Actions Artifacts](https://github.com/Kotlin/dokka/actions/workflows/preview-publish-ga.yml)
  builds the HTML API reference of several libraries like `kotlinx.coroutines`, and publishes the results as `zip` archive 
  artifacts. You can use it to preview your changes. The workflow is triggered for all commits.
* [Publish preview to web (S3)](https://github.com/Kotlin/dokka/actions/workflows/preview-publish-web-s3.yml)
  does the same thing as `Publish examples to GitHub Actions Artifacts`, but publishes the generated documentation
  to S3, so it can be accessed from any browser without the need to download anything. The web link will be printed
  under the `Print link` job step. This workflow is triggered by maintainer commits only as it requires encrypted 
  secrets to be run.

**Notes**:

* Some workflow job runs are flaky, but if more than a couple are failing at the same time or repeatedly - it indicates 
  a problem in the PR that needs to be addressed.
* While a Java version can be configured in the [`setup-java`](https://github.com/actions/setup-java#basic-configuration)
  action, it is not necessarily the version of Java that will be used to build/test the project. See the 
  [Java version](#java-version) section for more details.

### TeamCity

TeamCity is used for a subset of important / longer builds and checks, including artifact publication. 

The runs are triggered automatically by maintainer commits, or can be started manually by maintainers. External 
contributors can view the results in guest mode.

Notable builds:

* [Dokka Integration Tests](https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_Dokka_IntegrationTests)
  runs Dokka's integration tests, which are designed to test compatibility with different Kotlin versions, with different
  multiplatform targets and with various user scenarios.

### Gradle Build Scans

[Gradle Build Scans](https://scans.gradle.com/) can provide insights into a Dokka Build. 
JetBrains runs a [Gradle Develocity server](https://ge.jetbrains.com/scans?search.rootProjectNames=dokka).
that can be used to automatically upload reports.

To automatically opt in add the following to `$GRADLE_USER_HOME/gradle.properties`. 

```properties
org.jetbrains.dokka.build.scan.enabled=true
# optionally provide a username that will be attached to each report
org.jetbrains.dokka.build.scan.username=Hannah Clarke
```

A Build Scan may contain identifiable information. See the Terms of Use https://gradle.com/legal/terms-of-use/.

## Contacting maintainers

* If something cannot be done, not convenient, or does not work &mdash; submit an [issue](#submitting-issues).
* Discussions and general inquiries &mdash; use `#dokka` channel in 
  [Kotlin Community Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up).
