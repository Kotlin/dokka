# Workflow

Whether you're contributing a feature/fix to Dokka itself or developing a Dokka plugin, there are 3 essential things
you need to know how to do:

1. How to build Dokka or a plugin
2. How to use/test locally built Dokka in a project
3. How to debug Dokka or a plugin in IntelliJ IDEA

We'll go over each step individually in this section.

Examples below will be specific to Gradle and [Gradle’s Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html),
but you can apply the same principles and run/test/debug with CLI/Maven runners and build configurations if you wish.

## Build Dokka

Building Dokka is pretty straightforward:

```shell
./gradlew build
```

This will build all subprojects and trigger `build` in all composite builds. If you are working on a
[runner](https://github.com/Kotlin/dokka/tree/master/dokka-runners), you can build it independently.

Checks that are performed as part of `build` do not require any special configuration or environment, and should
not take much time (~2-5 minutes), so please make sure they pass before submitting a pull request.

### Troubleshooting build

#### API check failed for project ..

If you see a message like `API check failed for project ..` during the `build` phase, it indicates that the
[binary compatibility check](https://github.com/Kotlin/binary-compatibility-validator) has failed, meaning you've 
changed/added/removed some public API.

If the change was intentional, run `./gradlew apiDump` - it will re-generate `.api` files with signatures,
and you should be able to `build` Dokka with no errors. These updated files need to be committed as well. Maintainers
will review API changes thoroughly, so please make sure it's intentional and rational.

## Use / test locally built Dokka

Having built Dokka locally, you can publish it to `mavenLocal()`. This will allow you to test your changes in another
project as well as debug code remotely.

1. Publish a custom version of Dokka to Maven Local: `./gradlew publishToMavenLocal -Pversion=1.9.20-my-fix-SNAPSHOT`. 
   This version will be propagated to plugins that reside inside Dokka's project (`mathjax`, `kotlin-as-java`, etc),
   and its artifacts should appear in `~/.m2`
2. In the project you want to generate documentation for or debug on, add maven local as a plugin/dependency
   repository:
```kotlin
repositories {
   mavenLocal()
}
```
3. Update your Dokka dependency to the version you've just published:
```kotlin
plugins {
    id("org.jetbrains.dokka") version "1.9.20-my-fix-SNAPSHOT"
}
```

After completing these steps, you should be able to build documentation using your own version of Dokka.

## Debugging Dokka

Dokka is essentially a Gradle plugin, so you can debug it the same way you would any other Gradle plugin. 

Below you'll find instructions on how to debug Dokka's internal logic, but you can apply the same principles if you
wish to debug a Dokka plugin.

1. Choose a project to debug on, it needs to have some code for which documentation will be generated.
   Prefer using smaller projects that reproduce the exact problem or behaviour you want
   since the less code you have, the easier it will be to understand what's going on. You can use example projects
   found in [dokka/examples/gradle](https://github.com/Kotlin/dokka/tree/master/examples/gradle), there's both simple 
   single-module and more complex multi-module / multiplatform examples.
2. For the debug project, set `org.gradle.debug` to `true` in one of the following ways:

    * In your `gradle.properties` add `org.gradle.debug=true`
    * When running Dokka tasks:<br/>`./gradlew dokkaHtml -Dorg.gradle.debug=true --no-daemon`

3. Run the desired Dokka task with `--no-daemon`. Gradle should wait until you attach with debugger before proceeding
   with the task, so no need to hurry here.
   <br/>Example: `./gradlew dokkaHtml -Dorg.gradle.debug=true --no-daemon`.

4. Open Dokka in IntelliJ IDEA, set a breakpoint and, using remote debug in IntelliJ IDEA,
   [Attach to process](https://www.jetbrains.com/help/idea/attaching-to-local-process.html#attach-to-remote)
   running on the default port 5005. You can do that either by creating a `Remote JVM Debug` Run/Debug configuration
   or by attaching to the process via `Run` -> `Attach to process`

!!! note
    The reason for `--no-daemon` is that
    [Gradle daemons](https://docs.gradle.org/current/userguide/gradle_daemon.html) continue to exist even after the task
    has completed execution, so you might hang in debug or experience issues with `port was already in use` if you try
    to run it again.
    
    If you previously ran Dokka with daemons and you are already encountering problems with it, try killing
    gradle daemons. For instance, via `pkill -f gradle.*daemon`

In case you need to debug some other part of the build - consult the official Gradle
tutorials on [Troubleshooting Builds](https://docs.gradle.org/current/userguide/troubleshooting.html).


## Run integration tests

Dokka's [integration tests](https://github.com/Kotlin/dokka/tree/master/dokka-integration-tests) help check 
compatibility with various versions of Kotlin, Android, Gradle and Java. They apply Dokka to real user-like projects 
and invoke Gradle / Maven / CLI tasks to generate the documentation.

Integration tests require a significant amount of available RAM (~20-30GB), take 1+ hour and may require additional
environment configuration to run. For these reasons, it's not expected that you run all integration tests locally
as part of the everyday development process, they will be run on CI once you submit a PR.

However, if you need to run all integration tests locally, you can use the `integrationTest` task:

```shell
./gradlew integrationTest
```

If you need to run a specific test locally, you can run it from your IDE or by calling the corresponding Gradle
task (for example, `:dokka-integration-tests:gradle:testExternalProjectKotlinxCoroutines`).
