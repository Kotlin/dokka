[//]: # (title: Get started with Dokka)

Below you can find simple instructions to help you get started with Dokka.

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

> This guide applies to Dokka Gradle plugin (DGP) v2 mode. The DGP v1 mode is no longer supported. 
> To upgrade from v1 to v2 mode, follow the [Migration guide](dokka-migration.md).
>
{style="note"}

**Apply the Gradle Dokka plugin** 

Apply the Dokka Gradle plugin (DGP) in the root build script of your project:

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}
```

**Document multi-project builds**

When documenting [multi-project builds](https://docs.gradle.org/current/userguide/multi_project_builds.html),
apply the plugin to every subproject you want to document. Share Dokka configuration across subprojects 
by using one of the following approaches:

* Convention plugin
* Direct configuration in each subproject if you’re not using convention plugins

For more information about sharing Dokka configuration in multi-project builds, 
see [Multi-project configuration](dokka-gradle.md#multi-project-configuration).

**Generate documentation**

To generate documentation, run the following Gradle task:

```bash
./gradlew :dokkaGenerate
```

This task works for both single and multi-project builds.

Run the `dokkaGenerate` task from the aggregating project by prefixing the task with its project path (`:`). For example:

```bash
./gradlew :dokkaGenerate

// OR

./gradlew :aggregatingProject:dokkaGenerate
```

Avoid running `./gradlew dokkaGenerate` instead of `./gradlew: dokkaGenerate` or `./gradlew :aggregatingProject:dokkaGenerate`.
Without a project path (`:`) prefixing the task, Gradle tries to run all `dokkaGenerate` tasks across the entire build, which may trigger unnecessary work.

You can use different tasks to generate output in [HTML](dokka-html.md),
[Javadoc](dokka-javadoc.md) or both [HTML and Javadoc](dokka-gradle.md#configure-documentation-output-format).

> To learn more about using Dokka with Gradle, see [Gradle](dokka-gradle.md).
{style="tip"}

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

> This guide applies to Dokka Gradle plugin (DGP) v2 mode. The DGP v1 mode is no longer supported.
> To upgrade from v1 to v2 mode, follow the [Migration guide](dokka-migration.md).
>
{style="note"}

**Apply the Gradle Dokka plugin**

Apply the Gradle plugin for Dokka in the root build script of your project:

```groovy
plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}
```

**Document multi-project builds**

When documenting [multi-project builds](https://docs.gradle.org/current/userguide/multi_project_builds.html),
you need to apply the plugin to every subproject you want to document. Share Dokka configuration across subprojects
by using one of the following approaches:

* Convention plugin
* Direct configuration in each subproject if you’re not using convention plugins

For more information about sharing Dokka configuration in multi-project builds,
see [Multi-project configuration](dokka-gradle.md#multi-project-configuration).

**Generate documentation**

To generate documentation, run the following Gradle task:

```bash
./gradlew :dokkaGenerate
```

This task works for both single and multi-project builds.

Run the `dokkaGenerate` task from the aggregating project by prefixing the task with its project path. For example:

```bash
./gradlew :dokkaGenerate

// OR

./gradlew :aggregatingProject:dokkaGenerate
```

Avoid running `./gradlew dokkaGenerate` instead of `./gradlew :dokkaGenerate` or `./gradlew :aggregatingProject:dokkaGenerate`.
Without a project path (`:`) prefixing the task, Gradle tries to run all `dokkaGenerate` tasks across the entire build, which may trigger unnecessary work.

You can use different tasks to generate output in [HTML](dokka-html.md),
[Javadoc](dokka-javadoc.md) or both [HTML and Javadoc](dokka-gradle.md#configure-documentation-output-format).

> To learn more about using Dokka with Gradle, see [Gradle](dokka-gradle.md).
{style="tip"}

</tab>
<tab title="Maven" group-key="mvn">

Add the Maven plugin for Dokka to the `plugins` section of your POM file:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jetbrains.dokka</groupId>
            <artifactId>dokka-maven-plugin</artifactId>
            <version>%dokkaVersion%</version>
            <executions>
                <execution>
                    <phase>pre-site</phase>
                    <goals>
                        <goal>dokka</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

To generate documentation, run the `dokka:dokka` goal.

By default, the output directory is set to `target/dokka`.

To learn more about using Dokka with Maven, see [Maven](dokka-maven.md).

</tab>
</tabs>