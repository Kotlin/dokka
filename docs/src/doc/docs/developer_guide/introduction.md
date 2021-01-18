# Guide to Dokka Plugin development

## Building Dokka

Dokka is built with Gradle. To build it, use `./gradlew build`.
Alternatively, open the project directory in IntelliJ IDEA and use the IDE to build and run Dokka.

Here's how to import and configure Dokka in IntelliJ IDEA 2019.3:

* Select "Open" from the IDEA welcome screen, or File > Open if a project is
  already open
* Select the directory with your clone of Dokka
  
!!! note
    IDEA may have an error after the project is initally opened; it is OK
    to ignore this as the next step will address this error

* After IDEA opens the project, select File > New > Module from existing sources
  and select the `build.gradle.kts` file from the root directory of your Dokka clone
* After Dokka is loaded into IDEA, open the Gradle tool window (View > Tool
  Windows > Gradle) and click on the top left "Refresh all Gradle projects"
  button
  
  
## Configuration

tldr: you can use a convenient [plugin template](https://github.com/Kotlin/dokka-plugin-template) to speed up the setup.

Dokka requires configured `Kotlin plugin` and `dokka-core` dependency.

```kotlin
plugins {
    kotlin("jvm") version "<kotlin_version>"
}

dependencies {
    compileOnly("org.jetbrains.dokka:dokka-core:<dokka_version>")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
```

## Building sample plugin

In order to load a plugin into Dokka, your class must extend `DokkaPlugin` class. A fully qualified name of that class must be placed in a file named `org.jetbrains.dokka.plugability.DokkaPlugin` under `resources/META-INF/services`.
All instances are automatically loaded during Dokka setup using `java.util.ServiceLoader`.

Dokka provides a set of entry points, for which user can create their own implementations. They must be delegated using `DokkaPlugin.extending(definition: ExtendingDSL.() -> Extension<T, *, *>)` function,that returns a delegate `ExtensionProvider` with supplied definition. 

To create a definition, you can use one of two infix functions`with(T)` or `providing( (DokkaContext) -> T)` where `T` is the type of an extended endpoint. You can also use infix functions:

* `applyIf( () -> Boolean )` to add additional condition specifying whether or not the extension should be used
* `order((OrderDsl.() -> Unit))` to determine if your extension should be used before or after another particular extension for the same endpoint
* `override( Extension<T, *, *> )` to override other extension. Overridden extension won't be loaded and overridding one will inherit ordering from it.

Following sample provides custom translator object as a `DokkaCore.sourceToDocumentableTranslator`

```kotlin
package org.jetbrains.dokka.sample

import org.jetbrains.dokka.plugability.DokkaPlugin

class SamplePlugin : DokkaPlugin() {
    extension by extending {
        DokkaCore.sourceToDocumentableTranslator with CustomSourceToDocumentableTranslator
    }
}

object CustomSourceToDocumentableTranslator: SourceToDocumentableTranslator {
    override fun invoke(sourceSet: SourceSetData, context: DokkaContext): DModule
}
```

### Registering extension point

You can register your own extension point using `extensionPoint` function declared in `DokkaPlugin` class

```kotlin
class SamplePlugin : DokkaPlugin() {
    val extensionPoint by extensionPoint<SampleExtensionPointInterface>()
}

interface SampleExtensionPointInterface
```

### Obtaining extension instance

All registered plugins are accessible with `DokkaContext.plugin` function. All plugins that extends `DokkaPlugin` can use `DokkaPlugin.plugin` function, that uses underlying `DokkaContext` instance. If you want to pass context to your extension, you can obtain it using aforementioned `providing` infix function.

With plugin instance obtained, you can browse extensions registered for this plugins' extension points using `querySingle` and `query` methods:

```kotlin
    context.plugin<DokkaBase>().query { htmlPreprocessors }
    context.plugin<DokkaBase>().querySingle { samplesTransformer }
```

You can also browse `DokkaContext` directly, using `single` and `get` methods:

```kotlin
class SamplePlugin : DokkaPlugin() {

    val extensionPoint by extensionPoint<SampleExtensionPointInterface>()
    val anotherExtensionPoint by extensionPoint<AnotherSampleExtensionPointInterface>()

    val extension by extending {
        extensionPoint with SampleExtension()
    }

    val anotherExtension by extending { 
        anotherExtensionPoint providing { context ->
            AnotherSampleExtension(context.single(extensionPoint))
        }
    }
}

interface SampleExtensionPointInterface
interface AnotherSampleExtensionPointInterface

class SampleExtension: SampleExtensionPointInterface
class AnotherSampleExtension(sampleExtension: SampleExtensionPointInterface): AnotherSampleExtensionPointInterface
```

