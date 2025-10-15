[//]: # (title: Get started with Dokka)

Below you can find simple instructions to help you get started with Dokka.

<tabs group="build-script">
<tab title="Gradle Kotlin DSL" group-key="kotlin">

> Starting from Dokka 2.0.0, several configuration options, Gradle tasks, and steps to generate your documentation have been updated, including:
>
> * [Adjust configuration options](dokka-migration.md#adjust-configuration-options)
> * [Work with multi-module projects](dokka-migration.md#share-dokka-configuration-across-modules)
> * [Generate documentation with the updated tasks](dokka-migration.md#generate-documentation-with-the-updated-task)
> * [Specify an output directory](dokka-migration.md#output-directory)
>
> For more details and the full list of changes, see the [Migration guide](dokka-migration.md).
>
{style="note"}

Apply the Gradle plugin for Dokka in the root build script of your project:

```kotlin
plugins {
    id("org.jetbrains.dokka") version "%dokkaVersion%"
}
```

When documenting [multi-project](https://docs.gradle.org/current/userguide/multi_project_builds.html) builds, you need 
to apply the Gradle plugin within subprojects as well:

```kotlin
subprojects {
    apply(plugin = "org.jetbrains.dokka")
}
```

To generate documentation, run the following Gradle tasks:

* `dokkaHtml` for single-project builds
* `dokkaHtmlMultiModule` for multi-project builds

By default, the output directory is set to `/build/dokka/html` and `/build/dokka/htmlMultiModule`.

To learn more about using Dokka with Gradle, see [Gradle](dokka-gradle.md).

</tab>
<tab title="Gradle Groovy DSL" group-key="groovy">

Apply the Gradle plugin for Dokka in the root build script of your project:

```groovy
plugins {
    id 'org.jetbrains.dokka' version '%dokkaVersion%'
}
```

When documenting [multi-project](https://docs.gradle.org/current/userguide/multi_project_builds.html) builds, you need
to apply the Gradle plugin within subprojects as well:

```groovy
subprojects {
    apply plugin: 'org.jetbrains.dokka'
}
```

To generate documentation, run the following Gradle tasks:

* `dokkaHtml` for single-project builds
* `dokkaHtmlMultiModule` for multi-project builds

By default, the output directory is set to `/build/dokka/html` and `/build/dokka/htmlMultiModule`.

To learn more about using Dokka with Gradle, see [Gradle](dokka-gradle.md).

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