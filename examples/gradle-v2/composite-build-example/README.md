# Dokka Gradle Plugin Composite Build Example

This project demonstrates how to use Dokka Gradle Plugin to aggregate modules across a
[composite build projects](https://docs.gradle.org/current/userguide/composite_builds.html).

> [!WARNING]
> HTML is the only format that correctly supports multimodule aggregation.

### Summary

The example project has four included builds:

* [`build-logic`](build-logic) contains Kotlin/JVM and Dokka Gradle Plugin convention plugins.
* [`module-kakapo`](module-kakapo) and [`module-kea`](module-kea)
  (named after [New Zealand birds](https://en.wikipedia.org/wiki/Birds_of_New_Zealand))
  represent regular Kotlin/JVM projects.
* [`docs`](docs) aggregates the modules.

### Running

Run the `:docs:dokkaGenerate` Gradle task to generate documentation with the custom logo:

```bash
./gradlew :docs:dokkaGenerate
```

## Distinct module paths

> [!IMPORTANT]
> When Dokka Gradle Plugin aggregates modules, each module **must** have a distinct `modulePath`.
>
> When using composite builds, project paths may clash, so make sure to set a distinct `modulePath`.

The module path determines where each Dokka Module will be located within an aggregated
Dokka Publication.

By default, the module path is set to be the project path, which are distinct for a single
Gradle build. With composite builds the project paths may not be distinct, causing Dokka Gradle Plugin
to overwrite modules.

This can be achieved in a convention plugin.
[build-logic/src/main/kotlin/dokka-convention.gradle.kts](build-logic/src/main/kotlin/dokka-convention.gradle.kts). 
