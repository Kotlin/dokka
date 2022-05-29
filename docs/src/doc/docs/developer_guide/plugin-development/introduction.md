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
