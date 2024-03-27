# Introduction to plugin development

Dokka was built from the ground up to be easily extensible and highly customizable, which allows the community to 
implement plugins for missing or very specific features that are not provided out of the box.

Dokka plugins range anywhere from supporting other programming language sources to exotic output formats. You can add 
support for your own KDoc tags or annotations, teach Dokka how to render different DSLs that are found in KDoc 
descriptions, visually redesign Dokka's pages to be seamlessly integrated into your company's website, integrate 
it with other tools and so much more.

In order to have an easier time developing plugins, it's a good idea to go through
[Dokka's internals](../architecture/architecture_overview.md) first, to learn more about its
[data model](../architecture/data_model/documentable_model.md) and 
[extensions](../architecture/extension_points/extension_points.md).

## Setup

### Template 

The easiest way to start is to use the convenient [Dokka plugin template](https://github.com/Kotlin/dokka-plugin-template).
It has pre-configured dependencies, publishing and signing of your artifacts.

### Manual

At a bare minimum, a Dokka plugin requires `dokka-core` as a dependency:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "<kotlin_version>"
}

dependencies {
    compileOnly("org.jetbrains.dokka:dokka-core:<dokka_version>")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
}
```

In order to load a plugin into Dokka, your class must extend the `DokkaPlugin` class. A fully qualified name of that class
must be placed in a file named `org.jetbrains.dokka.plugability.DokkaPlugin` under `resources/META-INF/services`. 
All instances are automatically loaded during Dokka's configuration step using `java.util.ServiceLoader`.

## Extension points 

Dokka provides a set of entry points for which you can create your own implementations. If you are not sure which
extension points to use, have a look at [core extensions](../architecture/extension_points/core_extension_points.md) and
[base extensions](../architecture/extension_points/base_plugin.md).

You can learn how to declare extension points and extensions in
[Introduction to Extension points](../architecture/extension_points/extension_points.md).

In case no suitable extension point exists for your use case, do share the use case with the 
[maintainers](../community/slack.md) — it might be added in a future version of Dokka.

## Example

You can follow the [sample plugin tutorial](sample-plugin-tutorial.md), which covers the creation of a simple plugin 
that hides members annotated with your own `@Internal` annotation: that is, it excludes these members from the generated
documentation.

For more practical examples, have a look at sources of 
[community plugins](https://kotlinlang.org/docs/dokka-plugins.html#notable-plugins).

## Help

If you have any further questions, feel free to get in touch with Dokka's maintainers via [Slack](../community/slack.md) 
or [GitHub](https://github.com/kotlin/dokka).
