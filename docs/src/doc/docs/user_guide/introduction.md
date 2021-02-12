# Introduction

## Plugins 
Dokka can be customized with plugins. Each output format is internally a plugin.
Additionally, `kotlin-as-java` plugin can be used to generate documentation as seen from Java perspective. 
Currently maintained plugins are:

* `dokka-base` - the main plugin needed to run Dokka, contains html format
* `gfm-plugin` - configures `GFM` output format
* `jekyll-plugin` - configures `Jekyll` output format
* `javadoc-plugin` - configures `Javadoc` output format, automatically applies `kotlin-as-java-plugin` 
* `kotlin-as-java-plugin` - translates Kotlin definitions to Java 
* `android-documentation-plugin` - provides android specific enhancements like `@hide` support

Please see the usage instructions for each build system on how to add plugins to Dokka. 

## Source sets 
Dokka generates documentation based on source sets. 

For single-platform & multi-platform projects, source sets are the same as in Kotlin plugin:

 * One source set for each platform, eg. `jvmMain` or `jsMain`;
 * One source set for each common source set, eg. the default `commonMain` and custom ones like `jsAndJvmMain`.

When configuring multi-platform projects manually (eg. in the CLI or in Gradle without the Kotlin Gradle Plugin)
source sets must declare their dependent source sets. 
Eg. in the following Kotlin plugin configuration:

* `jsMain` and `jvmMain` both depend on `commonMain` (by default and transitively) and `jsAndJvmMain`;
* `linuxX64Main` only depends on `commonMain`. 

```kotlin
kotlin { // Kotlin plugin configuration
    jvm()
    js()
    linuxX64()

    sourceSets {
        val commonMain by getting {}
        val jvmAndJsSecondCommonMain by creating { dependsOn(commonMain) }
        val jvmMain by getting { dependsOn(jvmAndJsSecondCommonMain) }
        val jsMain by getting { dependsOn(jvmAndJsSecondCommonMain) }
        val linuxX64Main by getting { dependsOn(commonMain) }
    }
}
```

## Output formats
  Dokka documents Java classes as seen in Kotlin by default, with javadoc format being the only exception.

  * `html` - HTML format used by default
  * `javadoc` - looks like JDK's Javadoc, Kotlin classes are translated to Java
  * `gfm` - GitHub flavored markdown
  * `jekyll` - Jekyll compatible markdown

If you want to generate the documentation as seen from Java perspective, you can add the `kotlin-as-java` plugin
to the Dokka plugins classpath, eg. in Gradle:

```kotlin
dependencies{
    implementation("...")
    dokkaGfmPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${dokka-version}")
}
```

## Platforms

Each Dokka source set is analyzed for a specific platform. The platform should be extracted automatically from the Kotlin plugin.
In case of a manual source set configuration, you have to select one of the following:

  * `jvm`
  * `js`
  * `native`
  * `common` 
