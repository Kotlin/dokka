# Kotlin Playground Samples Plugin

Dokka plugin that makes @sample code blocks interactive and runnable using [Kotlin Playground](https://github.com/JetBrains/kotlin-playground).

## Applying the plugin
Plugin works out of the box without any additional configuration required. 

You can apply the Kotlin Playground Samples plugin the same way as other Dokka plugins:

<details open>
<summary>Kotlin</summary>

```kotlin
dependencies { 
    dokkaPlugin("org.jetbrains.dokka:kotlin-playground-samples-plugin")
}
```
</details>

<details>
<summary>Groovy</summary>

```groovy
dependencies {
    dokkaPlugin 'org.jetbrains.dokka:kotlin-playground-samples-plugin'
}
```
</details>

<details>
<summary>Maven</summary>

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    ...
    <configuration>
        <dokkaPlugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>kotlin-playground-samples-plugin</artifactId>
                <version>2.1.0</version>
            </plugin>
        </dokkaPlugins>
    </configuration>
</plugin>
```
</details>

## Configuration (Optional)
You can specify the Kotlin Playground JS script and the Kotlin Playground server. Otherwise, default values are used.

| Option                   | Description                                                                                              |
|--------------------------|----------------------------------------------------------------------------------------------------------|
| `kotlinPlaygroundScript` | URL to the Kotlin Playground JS script.                                                                  |
| `kotlinPlaygroundServer` | URL to the Kotlin Playground server for running and compiling samples. Used by Kotlin Playground script. |

### Configuration example using Dokka Gradle Plugin v2:
```kotlin
dokka {
    pluginsConfiguration {
        registerBinding(KotlinPlaygroundSamplesParameters::class, KotlinPlaygroundSamplesParameters::class)
        register<KotlinPlaygroundSamplesParameters>("KotlinPlaygroundSamplesPlugin") {
            kotlinPlaygroundScript = "https://customKotlinPlaygroundScript/example.js"
            kotlinPlaygroundServer = "https://kotlinPlaygroundServer.example.com/"
        }
    }
}

@OptIn(InternalDokkaGradlePluginApi::class)
abstract class KotlinPlaygroundSamplesParameters @Inject constructor(
    name: String
) : DokkaPluginParametersBaseSpec(
    name,
    "org.jetbrains.dokka.kotlinplaygroundsamples.KotlinPlaygroundSamplesPlugin",
) {
    @get:Input
    abstract val kotlinPlaygroundScript: Property<String>

    @get:Input
    abstract val kotlinPlaygroundServer: Property<String>

    override fun jsonEncode(): String {
        return """
        {
          "kotlinPlaygroundScript": "${kotlinPlaygroundScript.get()}",
          "kotlinPlaygroundServer": "${kotlinPlaygroundServer.get()}"
        }
        """.trimIndent()
    }
}
```
