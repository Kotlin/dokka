# Workflow

Whether you're contributing a feature/fix to Dokka itself or developing a separate plugin, there's 3 things
you'll be doing:

1. Building Dokka / Plugins
2. Using/Testing locally built Dokka in a (debug) project
3. Debugging Dokka / Plugin code

We'll go over each step individually in this section.

Examples below will be specific to Gradle and [Gradle’s Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html),
but you can apply the same principles and run/test/debug with CLI/Maven runners and build configurations if you wish.

## Building Dokka

Building Dokka is pretty straightforward, with one small caveat: when you run `./gradlew build`, it will run
integration tests as well, which might take some time and will consume a lot of RAM, so you would usually want
to exclude integration tests when building locally.

```shell
./gradlew build -x integrationTest
```

Unit tests which are run as part of `build` should not take much time, but you can also skip it with `-x test`.

### Troubleshooting build

#### API check failed for project ..

If you see messages like `API check failed for project ..` during `build` phase, it indicates that
[binary compatibility check](https://github.com/Kotlin/binary-compatibility-validator) has failed, meaning you've 
changed/added/removed some public API.

If the change was intentional, run `./gradlew apiDump` - it will re-generate `.api` files with signatures,
and you should be able to `build` Dokka with no errors. These updated files need to be committed as well. Maintainers
will review API changes thoroughly, so please make sure it's intentional and rational.

## Using/testing locally built Dokka

Having built Dokka locally, you can publish it to `mavenLocal()`. This will allow you to test your changes in another
project as well as debug code remotely.

1. Change `dokka_version` in `gradle.properties` to something that you will use later on as the dependency version.
   For instance, you can set it to something like `1.7.20-my-fix-SNAPSHOT`. This version will be propagated to plugins
   that reside inside Dokka's project (such as `mathjax`, `kotlin-as-java`, etc).
2. Publish it to maven local (`./gradlew publishToMavenLocal`). Corresponding artifacts should appear in `~/.m2`
3. In the project you want to generate documentation for or debug on, add maven local as a plugin/dependency
   repository:
```kotlin
repositories {
   mavenLocal()
}
```
4. Update your dokka dependency to the version you've just published:
```kotlin
plugins {
    id("org.jetbrains.dokka") version "1.7.20-my-fix-SNAPSHOT"
}
```

After completing these steps, you should be able to build documentation using your own version of Dokka.

## Debugging Dokka

Dokka is essentially a gradle plugin, so you can debug it the same way you would any other gradle plugin. 

Below you'll find instructions on how to debug Dokka's internal logic, but you can apply the same principles if you
wish to debug a plugin which resides in a separate project.

1. Choose a project to debug on, it needs to have some code for which documentation will be generated.
   Prefer using smaller projects that reproduce the exact problem or behaviour you want
   since the less code you have, the easier it will be to understand what's going on. You can use example projects
   found in [dokka/examples/gradle](https://github.com/Kotlin/dokka/tree/master/examples/gradle), there's both simple 
   single-module and more complex multimodule/multiplatform examples.
2. For the debug project, set `org.gradle.debug` to `true` in one of the following ways:

    * In your `gradle.properties` add `org.gradle.debug=true`
    * When running Dokka tasks:<br/>`./gradlew dokkaHtml -Dorg.gradle.debug=true --no-daemon`

3. Run desired Dokka task with `--no-daemon`. Gradle should wait until you attach with debugger before proceeding
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
