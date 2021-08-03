# Guide to Dokka Plugin development
  
## Plugin setup

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

Dokka provides a set of entry points, for which user can create their own implementations. 
Those entry points are called `extension points` and are made in order to provide plugability and customizable behaviour.
They allow plugin creators to insert their implementations or new functionalities into existing framework, adjusting the documentation to their needs.
All extension points must be delegated using `DokkaPlugin.extending(definition: ExtendingDSL.() -> Extension<T, *, *>)` function,that returns a delegate `ExtensionProvider` with supplied definition. 

To create a definition, you can use one of two infix functions`with(T)` or `providing( (DokkaContext) -> T)` where `T` is the type of an extended endpoint. You can also use infix functions:

* `applyIf( () -> Boolean )` to add additional condition specifying whether the extension should be used
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

### Composing multiple plugins

It is reasonable to assume that the end user will apply multiple different plugins to theirs documentation. 
What is more, this is happening at every Dokka run, since most of the default implementations are contained within separate plugins that are automatically applied.

Plugins can depend on other plugins and customise their behaviour. 
For example, `GfmPlugin` creates its own implementation of the `Renderer` interface:
```kotlin
class GfmPlugin : DokkaPlugin() {
    val renderer by extending {
        CoreExtensions.renderer providing ::CommonmarkRenderer
    }
}
```

One might want to create a slightly different implementation but still based around the same concept. 
To do that we can depend on `GfmPlugin` to create a `JekyllPlugin` with different renderer.
In order to make it happen we add a dependency on `GfmPlugin` in our build tool of choice and create a plugin that overrides previous behaviour:

```kotlin
class JekyllPlugin : DokkaPlugin() {
    val renderer by extending {
        (CoreExtensions.renderer
                providing { JekyllRenderer(it) }
                override plugin<GfmPlugin>().renderer)
    }
}
```

While designing a plugin one should consider a number of things:

* Is my plugin specific to only my / my company's workflow?
* Can other users combine it with other plugins?
* Can other people use it as a basis of their plugins?

In the ideal world a plugin, just like a good library, should provide a decent API to use it as a basis of other plugins, 
provide a logic that has enough customisation to be applicable for other use-cases and be composable with other plugins.

