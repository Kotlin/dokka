# Dokka Gradle Plugin DSL 2.0 - RAW NOTES

## dokka usage on GitHub

| Project                                             | Short description                                                                                                                                    | Dokka version | Category          | Formats | Single/Multi module | Platform(s)    |
|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|-------------------|---------|---------------------|----------------|
| https://github.com/spring-projects/spring-framework | minimal config, a lot of external docs(remote package-lists), custom output dirs, shared script with configuration                                   | 1.8.20        | Server, Framework | HTML    | Multi               | JVM            |
| https://github.com/square/okhttp                    | configuration via subprojects in root                                                                                                                | 1.9.10        | Library           | HTML    | Multi               | JVM            |
| https://github.com/Kotlin/kotlinx.coroutines        | convention plugin, external docs(local package-lists), templates, knit plugin, source links                                                          | 1.9.10        | library           | html    | multi               | kmp            |
| https://github.com/bumptech/glide                   | mostly java, custom styles/assets, subprojects in root with explicit list of projects, has java only modules, needs sorting of modules in navigation | 1.8.20        | library           | html    | multi               | jvm            |
| https://github.com/realm/realm-kotlin               | multiple includes, HTML published to maven with `dokka` classifier, explicit copy-paste in project configuration                                     | 1.9.0         | library           | html    | multi               | kmp            |
| https://github.com/airbnb/lottie-android            | applied BUT NOT USED :)                                                                                                                              | 1.8.20        |                   |         |                     |                |
| https://github.com/square/leakcanary                | root project config                                                                                                                                  | 1.8.10        | library           | GFM     | multi(collector)    | jvm            |
| https://github.com/element-hq/element-android       | minimal config                                                                                                                                       | 1.8.10        | library           | html    | single              | android        |
| https://github.com/InsertKoinIO/koin                | dokka as javadoc for maven ONLY, explicit apply in module, no configuration                                                                          | 1.8.10        | library           | html    | multi               | kmp(+jvm-only) |
| https://github.com/coil-kt/coil                     |                                                                                                                                                      | 1.9.10        | library           | html    | multi               | kmp            |
| https://github.com/ktorio/ktor                      | versioning,                                                                                                                                          |               |                   |         |                     |                |
| https://github.com/square/okio                      | used as javadoc in maven with GFM format                                                                                                             |               |                   |         |                     |                |
| https://github.com/apache/jmeter                    | mostly java, used as javadoc for maven in modules with kotlin sources                                                                                |               |                   |         |                     |                |
| https://github.com/pinterest/ktlint                 | used as javadoc for maven                                                                                                                            |               |                   |         |                     |                |
| https://github.com/apollographql/apollo-kotlin      | both html and javadoc to maven                                                                                                                       |               |                   |         |                     |                |
| https://github.com/detekt/detekt                    | 2 HTML artifacts produced: multi-module for core apis + separate single-module for gradle plugin                                                     |               |                   |         |                     |                |
| https://github.com/JetBrains/xodus                  | javadoc                                                                                                                                              |               |                   |         |                     |                |
| https://github.com/mamoe/mirai                      |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/firebase/firebase-android-sdk    | uses Dackka :)                                                                                                                                       |               |                   |         |                     |                |
| https://github.com/getsentry/sentry-java            |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/ACRA/acra                        | javadoc inside project                                                                                                                               |               |                   |         |                     |                |
| https://github.com/ingokegel/jclasslib              | html, javadoc, etc...                                                                                                                                |               |                   |         |                     |                |
| https://github.com/vanniktech/Emoji                 | not published, no config                                                                                                                             |               |                   |         |                     |                |
| https://github.com/square/moshi                     | almost the same as okio + HTML                                                                                                                       |               |                   |         |                     |                |
| https://github.com/MobileNativeFoundation/Store     |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/google/accompanist               |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/mockk/mockk                      | javadoc format as javadoc for maven in JVM project + html format as javadoc for maven in KMP project                                                 |               |                   |         |                     |                |
| https://github.com/skydoves/TransformationLayout    |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/google/ksp                       |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/ExpediaGroup/graphql-kotlin      |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/lavalink-devs/Lavalink           |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/mikepenz/AboutLibraries          |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/skydoves/Balloon                 |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/facebook/facebook-android-sdk    | as javadoc, additional source roots from other modules (like collector)                                                                              |               |                   |         |                     |                |
| https://github.com/mapbox/mapbox-maps-android       | complex setup: TODO review later                                                                                                                     |               |                   |         |                     |                |
| https://github.com/hexagonkt/hexagon                | includes all tests as samples, custom plugin used (mermaid)                                                                                          |               |                   |         |                     |                |
| https://github.com/JetBrains/gradle-intellij-plugin |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/ajalt/clikt                      |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/Kotlin/kotlinx-io                |                                                                                                                                                      |               |                   |         |                     |                |
| https://github.com/recloudstream/cloudstream        | it's an actual APP, HTML is published - no idea why                                                                                                  |               |                   |         |                     |                |
| https://github.com/mongodb/mongo-java-driver        | no aggregation - every module published to separate html page                                                                                        |               |                   |         |                     |                |

dokkatoo usage:

| Project                                                    | Short description                                                                                                                                                                                                                                                               |
|------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| https://github.com/diffplug/selfie                         | html website                                                                                                                                                                                                                                                                    |
| https://github.com/oss-review-toolkit/ort                  | javadoc to maven                                                                                                                                                                                                                                                                |
| https://github.com/gradle/gradle                           | creation of custom dokka sourceSets, hacks to change header name (comparing to module name), https://github.com/gradle/gradle/blob/592f269cc959c3620413391e1dad68695e7adad2/build-logic/documentation/src/main/groovy/gradlebuild/docs/GradleKotlinDslReferencePlugin.java#L138 |
| https://github.com/Kantis/ks3                              | sourceSetScope/modulePath?                                                                                                                                                                                                                                                      |
| https://github.com/GW2ToolBelt/api-generator               | javadoc(maven) + html(website)                                                                                                                                                                                                                                                  |
| https://github.com/diffplug/blowdryer-diffplug             | html or javadoc for maven - looks like something doesn't work, but no one cares what is published to maven central in javadoc artifact :)                                                                                                                                       |
| https://github.com/EdricChan03/androidx-ktx-extras         | external links                                                                                                                                                                                                                                                                  |
| https://github.com/adamko-dev/kotka-streams                |                                                                                                                                                                                                                                                                                 |
| https://github.com/evant/gradle-central-release-publishing | html(maven)                                                                                                                                                                                                                                                                     |

project applying and configuring: explicit, via allprojects, via convention plugins

if there are multiple formats -- configuration is mostly common between formats
javadoc is used for maven central even for multiplatform projects — html, javadoc and GHM formats are used ^_^

almost everywhere the workflow is to use tasks.dokka.configureEach { dokkaSourceSets.configureEach { ... } }

changing output dir is a frequent thing to do

small amount of projects uses `sourceLink` or configured wrong (wrong paths)

A lot of projects uses `perPackage` as `glob-like` (wildcards), not as regex.
So instead of something like `com\\.example\\.internal` they just write `com.example.internal`,
it works well (in all cases I think), as `.` in regex is just a match for any character.
Though, if we start matching with `*` the behavior could be different:

* `com.example.internal.*` - works acceptably the same for both glob and regex
* `com.example.internal*` - works differently (glob - fine, regex - match `l` character multiple times)
* `.*internal.*` - works fine in regex, will not work in glob (because of first symbol is `.`)
* `*internal*` - incorrect regex, works fine with glob

api poc:

```kotlin
dokka {
    sourceSets.configureEach { /*...*/ }

    formats.html {
        // base plugin configuration
    }

    formats {
        kdoc() // similar to javadoc - toolable and publisheble to maven-central with -kdoc classifier (enabled by default when it will be ready)
        html() // enabled by default, gradle property to disable it by default (similar to how KGP adds stdlib dependency by default)
        javadoc()

        html {
            enabled = false // f.e. can be disabled in some project (?)
        }
    }

    aggregate.includeSubprojects(exclude = listOf("name1", "name2"))
    aggregate.includeAllprojects(exclude = listOf("name1", "name2")) // includes itself (?)
    aggregate.includeSubprojects(
        // can be called multiple times
        include = listOf("name1"),
        exclude = listOf("name1"),
    )

    // TODO: multi-module vs collector selection
    aggregation {
        // multiModule is default
        multiModule {
            // includes?
            includes.from("") // aggregation includes
            fileLayout = NoCopy
        }
        merged()
        mode = MultiModule // Merged
        includeProjects(
            "any name of the project, even not subproject",
            "a2",
            "a5"
        )
        includeSubprojects() // all subprojects
        includeSubprojects(name1, name2)
    }

    multiModule {
        includeSubprojects()
    }

    merged {
        includeSubprojects()
    }

    collector {
        includeSubprojects()
    }


// extensionAware/namedcollection
    plugins {
        // provided by some gradle plugin
        // applied via:
        // plugins {
        //   id("org.jetbrains.dokka")
        //   id("org.example.dokka-mermaid")
        // }
        mermaid {

        }

        // can we provide configuration in some other way 
        configure("org.jetbrains.dokka:javadoc-kotlinAsJava:1.9.10") {

        }
    }

// configurable file collection
    includes.from("")
}

// for JVM projects
kotlin {
    withJavadocJar() // or withDokkaJavadocJar() - similar to java api 
}

// fro MPP projects
kotlin {
    withJavadocJar()
}

// java plugin configuration
java {
    withJavadocJar()
    withSourceJar()
}

```

```kotlin

dokka { // DokkaProjectConfiguration
    formats.html {

    }

    // for current module
    generation { // DokkaGenerationConfiguration: DokkaModuleConfiguration
        moduleName = ""
        moduleVersion = "1.2.3"

        formats
    }

    // for aggregate module
    aggregation {

        formats
    }
}

```

```kotlin
// rootProject.build.kts

// aggregate configuration
dokka {
    aggregation {
        includeProjects(project, vararg)
        includeSubprojects(
            include = subprojects,
            exclude = listOf("123, 123")
        )
        includeSubprojects {
            // these configurations are not compatible with Isolated Projects
            // one more possible problem - if child-project is included in multiple aggregations and both inherits  
            inheritConfiguration = true // https://github.com/gradle/gradle/issues/25179
            applyDokkaPlugin = true // https://github.com/gradle/gradle/issues/22514
        }
    }
}

dependencies {
    dokkaAggregation(project(""))
    // project from included build
    dokkaAggregation("group:artifact")
}

```

Minimal example with aggregation:

```kotlin
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    perModule("name or regex") {

    }
    perSourceSet("name or regex") {

    }
    perPackage("name or regex") {

    }

    aggregation {
        includeSubprojects(
            exclude = listOf("common-utils")
        ) {
            inheritConfiguration = true
            applyDokkaPlugin = true
        }
    }
}
```

tasks:

* dokkaGenerate - will generate documentation for all formats / all aggregations
* dokkaGenerateHtml - will generate documentation for all HTML things
* dokkaGenerateModuleHtml - will generate documentation for this module only
* dokkaGenerateModulePartialHtml - will generate documentation for this module only, which will be used for aggregation
* dokkaGenerateAggregatedHtml - will generate aggregated HTML from included projects
* dokkaGenerateModuleHtmlJar - jar for module html
* dokkaGenerateAggregatedHtmlJar - TODO is it needed?
* replace `Html` with any other format name for other formats

* dokkaGenerateJson —- machine compatible format
* dokkaGenerateJavadoc — only for JVM projects / jvm target in KMP project (?)
* dokkaGenerateJavadocJar — only for JVM projects
* dokkaGenerate{FORMAT}
* dokkaDumpConfiguration — could be useful for custom processing, tools, etc — may be not really needed.

javadoc frequent usage:

```kotlin
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")

    // this
    from("$buildDir/dokka/javadoc")
    dependsOn(tasks.named("dokkaJavadoc"))
    // or
    // from(tasks.named("dokkaJavadoc"))
}
```

new dsl:

```kotlin
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(dokka.formats.html.outputDirectory) // TODO: will it work as dependency - most likely not?
}
```

Check how it will affect https://github.com/vanniktech/gradle-maven-publish-plugin

Check that we can aggregate documentation from included builds

How to achieve this???

    The configuration of the root project must be propagated to all of the subprojects. 
    If a subproject has a different configuration, it must be used instead of the root one.

Migration (2.0.0 = new plugin release version):

* 2.0.0 — 2 DSLs are in 1 plugin - old API deprecated with warning, deprecated tasks start to show warning
* 2.1.0 — same - old API deprecated with error (but still there will be no red code),
  deprecated tasks start to fail with error on execution (if f.e. deprecation is suppressed, or groovy is used)
* 2.2.0 — old API removed, old tasks removed

Additional: Migration from dokkatoo would be nice

Isolated Projects: https://gradle.github.io/configuration-cache/#status_as_of_gradle_8_5

Javadoc(and not) jar generation usecases:

* generate `jar` with `javadoc` classifier:
    * content could be of any format (javadoc, html, gfm)
* generate `jar` with `html`/`html-docs` classifier:
    * content contains `html`

What about the settings plugin?
We can put there some common configuration flags, that could be then applied to packages/sourceSets/modules(projects)
This configuration will be inherited in projects.
Dokka plugin can be automatically applied everywhere where `org.jetbrains.kotlin.*` plugins applied
(if that's possible), additionally ignored/explicit list of projects to filter
For this, better to create separate sourceSets (with different dependencies), as settings plugin MUST only depend on
kotlin-stdlib (embedded in Gradle)

What should we do documentation with `root/aggregate` projects - should we have it included in docs?
F.e following project structure:

```
root/
  build.gradle.kts
  setings.gradle.kts
  src/main/kotlin/RootClass.kt
  subproject-a/
    build.gradle.kts
    src/main/kotlin/AClass.kt
  subproject-b/
    build.gradle.kts
    src/main/kotlin/BClass.kt
```

Should dokka generate documentation for two (subproject-a + subproject-b) or three (additionally for root) modules?

two main usages of dokka:

* generate jar with documentation for **MODULE**
* generate html/gfm with documentation for **ALL MODULES** (merged)

---

Sharing of configuration possibilities:

1. no sharing, explicit logic in every subproject -> errorprone
2. sharing via `subprojects/allprojects` in `rootProject/some other project` -> not compatible with Isolated Projects
3. sharing via `convention plugins` -> good and fine, but boilerplate for simple projects without buildSrc (or similar)
4. sharing via `settings plugin` -> should work fine, no boilerplate, easy but not very popular;
   configuration is split in multiple places (settings vs root project vs subprojects),
   AGP 8.2 also added `settings plugin`,
   version catalogs don't work with settings plugins

TODO:

* jar generation

Dokka Gradle Runner / Dokka Gradle Plugin

Dokka Engine Plugin

There are multiple types of Dokka Engine Plugins:

* applied to format -> f.e. mathjax/mermaid will generate code which will work only for HTML
* applied to dist -> f.e. versioning (for html only, but could be adapted to other formats) will operate on
  final built dist -> can be both aggregated and single module
* applied everywhere -> f.e. kotlinAsJava will change signatures, will work in any context
* should be applied to both format and dist -> f.e. base (html format),
  styleSheet names should be used during module(format) gen,
  styleSheet files should be used during multi-module(dist) gen

multiModule is plugin, but should not

Gradle Plugin ids:

* dokka.base -> will not create any sourceSets based on Kotlin plugin, no tasks created, just configuration
* dokka -> will create sourceSets based on Kotlin plugins
* dokka.settings -> will create shared configuration + apply dokka plugin where kotlin plugin is applied

mpp vs jvm projects