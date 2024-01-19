# Dokka Gradle Plugin DSL 2.0 - single module projects

# 1. Applying Dokka Gradle Plugin

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}
```

# 2. Running Dokka Gradle Plugin tasks

Out-of-the-box without any additional configuration the task will build Dokka HTML documentation in `build/dokka`

`./gradlew dokkaBuild` - single task to launch Dokka Engine.

Possible task names:

* dokkaGenerate
* generateDokka
* dokkaBuild — (Oleg's favorite)
* buildDokka
* dokkaAssemble
* assembleDokka
* ???

# 3. Adding Dokka Engine Plugins

Different Dokka Engine Plugins may or may not have configuration so both cases should be considered.

Here are how different options will work for both cases.

## 3.1 Adding plugin WITHOUT configuration

Option 1: via Gradle configurations (as in dokkatoo):

```kotlin
dependencies {
    dokkaPlugin("dependency")
    // or
    dokkaPlugin(libs.dokka.kotlinAsJava)
}
```

Option 2: via custom dokka DSL (Oleg's favorite):

```kotlin
dokka {
    plugin("group:artifact:version")
    // or
    plugin(libs.dokka.kotlinAsJava)
}
```

> Under the hood we will do the same as calling `dokkaPlugin` in `dependencies`, but it will be better discoverable and
> nearer to other dokka configuration.

Both options should support adding plugin via `group:artifact:version` String notation and via Gradle Version Catalogs.

## 3.2 Only configuring plugin (if it's added somewhere else)

We should be careful with such blocks as it could be error-prone because users might think that configuring plugin will
be enough to make it work.
Better to have ability and suggest to add and configure plugin in one go.
Configuring plugin without directly adding it can be considered as advanced use-case.

Option 1:

```kotlin
dokka {
    configurePlugin(className = "com.glureau.HtmlMermaidDokkaPlugin") {
        property("lightTheme", "forest")
        property("darkTheme", "dark")
    }
    // or some other name
    pluginConfiguration(className = "com.glureau.HtmlMermaidDokkaPlugin") { ... }
}
```

## 3.3 Adding plugin WITH configuration

Option 1: via Gradle configurations (as in dokkatoo):

```kotlin
dependencies {
    dokkaPlugin(libs.dokka.kotlinAsJava)
}

dokka {
    configurePlugin(className = "com.glureau.HtmlMermaidDokkaPlugin") {
        property("lightTheme", "forest")
        property("darkTheme", "dark")
    }
    // or some other name
    pluginConfiguration(className = "com.glureau.HtmlMermaidDokkaPlugin") { ... }
}
```

Option 2: via custom DSL (v1)

```kotlin
dokka {
    applyPlugin(
        dependency = libs.dokka.kotlinAsJava,
        className = "com.glureau.HtmlMermaidDokkaPlugin",
    ) {
        property("lightTheme", "forest")
        property("darkTheme", "dark")
    }
    // or some other name
    plugin(...) { ... }
}
```

Option 3: via custom DSL (v2)

```kotlin
dokka {
    plugin(libs.dokka.kotlinAsJava) {
        // single dependency can provide multiple configurable plugins
        configuration(pluginClassName = "com.glureau.HtmlMermaidDokkaPlugin") {
            property("lightTheme", "forest")
            property("darkTheme", "dark")
        }
    }
}
```

Option 4: via custom DSL (v3) (Oleg's favorite)

```kotlin
dokka {
    plugin(libs.dokka.kotlinAsJava)
    pluginConfiguration("com.glureau.HtmlMermaidDokkaPlugin") {
        property("lightTheme", "forest")
        property("darkTheme", "dark")
    }

    // or plugin + pluginConfiguration in one go (shortcut)
    plugin(libs.dokka.kotlinAsJava, "com.glureau.HtmlMermaidDokkaPlugin") {
        property("lightTheme", "forest")
        property("darkTheme", "dark")
    }
}
```

Option 5: via custom DSL using Gradle constructs (can be used as under-the-hood implementation for previous DSL options)

```kotlin
dokka {
    plugins.add(libs.dokka.kotlinAsJava)
    pluginConfigurations.register("com.glureau.HtmlMermaidDokkaPlugin") {
        property("lightTheme", "forest")
        property("darkTheme", "dark")
    }
}
```

## Possible issues:

* function name `property(name, value)` can clash with `Project.property(name)` which is implicitly imported in
  Gradle scripts:
  ```kotlin
  dokka {
      plugin("") {
          property("L") // will compile and resolve to `project.property("L")`
      }
  }
  ```
  replacement could be:
    * `pluginProperty`
    * `intProperty`/`longProperty`/etc.
    * `parameter`/`param`

# 4. configuration

## Root configuration

```kotlin
dokka {
    // global properties
    dokkaEngineVersion.set("1.9.20")
    offlineMode.set(false)
    warningsAsErrors.set(false)

    // module level properties
    moduleName.set(project.name)
    moduleVersion.set(project.version.toString())
    suppressObviousFunctions.set(false)
    suppressInheritedMembers.set(false)

    // sourceSet level properties
    includeEmptyPackages.set(true)

    includedDocumentation.from("includes.md") // ConfigurableFileCollection
    // optional shortcuts
    // includes text directly (not via module documentation file format)
    moduleDocumentation("This is an example of module")
    // includes text directly from file (not via module documentation file format)
    moduleDocumentationFrom("docs/module.md")
    // same but for specific package
    packageDocumentation("org.example.internal", "don't use")
    packageDocumentationFrom("org.example.internal", file("docs/internal.md"))

    // package level properties
    documentedVisibilities.set(setOf(PUBLIC, INTERNAL))
    warnOnUndocumented.set(true)
    includeDeprecated.set(false)
}
```

## SourceSet configuration

```kotlin
dokka {
    // configuring all sourceSets
    sourceSets.configureEach {
        // sourceSet specific properties, those will be set by DGP from information retreived from KGP
        suppress.set(false)
        displayName.set(name)

        platform.set(KotlinPlatformType.jvm)
        jdkVersion.set(8) // TODO: only used for jvm and android 
        languageVersion.set(KotlinVersion.KOTLIN_2_1) // or just string?
        apiVersion.set(KotlinVersion.KOTLIN_2_1)

        classpath.from(file("libs/dependency.jar")) // ConfigurableFileCollection
        sourceFiles.from(file("src")) // ConfigurableFileCollection
        suppressedSourceFiles.from() // ConfigurableFileCollection
        samples.from("samples/Basic.kt", "samples/Advanced.kt") // ConfigurableFileCollection


        // properties below will override same properties declared in `root` configuration

        // sourceSet level properties 
        includeEmptyPackages.set(true)

        includedDocumentation.from("includes.md") // ConfigurableFileCollection
        // `module*` naming can be confusing here (in sourceSet)
        // optional shortcuts
        // includes text directly (not via module documentation file format)
        moduleDocumentation("This is an example of module")
        // includes text directly from file (not via module documentation file format)
        moduleDocumentationFrom("docs/module.md")
        // same but for specific package
        packageDocumentation("org.example.internal", "don't use")
        packageDocumentationFrom("org.example.internal", file("docs/internal.md"))

        // package level properties
        documentedVisibilities.set(setOf(PUBLIC, INTERNAL))
        warnOnUndocumented.set(true)
        includeDeprecated.set(false)
    }
    // or via pattern - `*` is a glob which matches all entries by name  
    perSourceSet("*") { /* same options available as above */ }

    // configuring part of the sourceSets
    sourceSets.matching { it.name.startsWith("macos") }.configureEach { /* same options available as above */ }
    // or via pattern
    perSourceSet("macos*") { /* same options available  as above */ }

    // configuring specific sourceSet
    sourceSets.named("jvmMain") { /* same options available as above */ }
    // or via pattern
    perSourceSet("jvmMain") { /* same options available as above */ }
}
```

Something to think:

* patterns - regex vs glob
    * regex could be more poverfull, but glob is easier to write
    * glob is used in more places in Gradle itself (f.e. `testFilters`)
    * A lot of projects uses `perPackage` as `glob-like` (wildcards), not as regex.
      So instead of something like `com\\.example\\.internal` they just write `com.example.internal`,
      it works well (in all cases I think), as `.` in regex is just a match for any character.
      Though, if we start matching with `*` the behavior could be different:
        * `com.example.internal.*` - works acceptably the same for both glob and regex
        * `com.example.internal*` - works differently (glob - fine, regex - match `l` character multiple times)
        * `.*internal.*` - works fine in regex, will not work in glob (because of first symbol is `.`)
        * `*internal*` - incorrect regex, works fine with glob

## perPackage configuration

```kotlin
dokka {
    // same glob pattern as in `perSourceSet`
    perPackage("*.internal.*") {
        // package specific properties
        suppress.set(false)

        packageDocumentation.from("includes-internal.md") // ConfigurableFileCollection
        // optional shortcuts
        // includes text directly (not via module documentation file format)
        packageDocumentation("This package is internal and should not be used")
        // includes text directly from file (not via module documentation file format)
        packageDocumentationFrom("docs/internal.md")

        // properties below will override same properties declared in `root` or `sourceSet` configuration

        // package level properties
        documentedVisibilities.set(setOf(PUBLIC, INTERNAL))
        warnOnUndocumented.set(true)
        includeDeprecated.set(false)
    }

    // can be aslo configured for sourceSet (via any configuration option)
    sourceSets.named("jvmMain") {
        perPackage("*internal*") { /* same options available as above */ }
    }
    // or
    perSourceSet("macos*") {
        perPackage("*internal*") { /* same options available as above */ }
    }
}
```

## SourceLink configuration

By default `localDirectory` points to root of the project, so that declaring remoteUrl to the root of the project is
fine. We can also add validation for this URL, at least for known VCS (GitHub and etc) so that it at least point to
something feesible like `http(s)://www.github.com/{OWNER}/{REPO}/tree/{SOMETHING}`.
`localDirectory` can be both absolute/relative path, if relative it's resolved based on root project path like in
`file("...")`.
Same for `remoteLineSuffix` we can set default value based on URL.

```kotlin
dokka {
    sourceLink("https://www.github.com/owner/repository/tree/main")
    // or 
    sourceLink {
        remoteUrl.set("https://www.github.com/owner/repository/tree/main")
        localDirectory.set(project.rootDir)
        remoteLineSuffix.set("#L")
    }
    // or
    sourceLink("https://www.github.com/owner/repository/tree/main") {
        localDirectory.set(project.rootDir)
        remoteLineSuffix.set("#L")
    }

    // same can be configured per sourceSet if needed

    sourceSets.named("jvmMain") {
        sourceLink {
            // no link will be generated if set to null
            remoteUrl.set(null)
        }
    }
}
```

## ExternalDocumentationLink configuration

```kotlin
dokka {
    // the simplest case when no package-list url provided and it's infered from url
    externalDocumentationLink("kotlinlang.org/api/kotlinx.coroutines")
    // remote package list url
    externalDocumentationLink(
        "kotlinlang.org/api/kotlinx.coroutines",
        "kotlinlang.org/api/kotlinx.coroutines/somewhere/package-list"
    )
    // local package list file
    externalDocumentationLinkFrom(
        "kotlinlang.org/api/kotlinx.coroutines",
        file("build/downloaded/package-list")
    )

    // some predefined useful links, can be enabled or disabled (by default `enabled=true`)
    externalDocumentationLinkToKotlinStdlib(enabled = false)

    // this could be useful also (by default `enabled=true` and can be omitted)
    externalDocumentationLinkToKotlinxCoroutines()

    // if for some reason, some of the link becomes outdated it can be fixed by lambda
    externalDocumentationLinkToKotlinxSerialization {
        // via remote url
        packageListUrl("kotlinlang.org/api/kotlinx.serialization/somewhere-new/package-list")
        // or via local file
        packageListFrom(file("build/downloaded/package-list"))
    }

    // fully custom
    externalDocumentationLink {
        url.set("kotlinlang.org/api/kotlinx.coroutines")
        packageListUrl("kotlinlang.org/api/kotlinx.serialization/somewhere-new/package-list")
        // or
        packageListFrom(file("build/downloaded/package-list"))
        // or
        packageListLocation.set("kotlinlang.org/api/kotlinx.serialization/somewhere-new/package-list".toURI())
        // or
        packageListLocation.set(file("build/downloaded/package-list").toURI())
    }

    // same options can be configured per sourceSet if needed

    sourceSets.named("androidMain") {
        externalDocumentationLinkToAndroidSdk()
    }
}
```

# 5. HTML format

Enabled by default, HTML format configuration is accessible under `dokka.html` sub-DSL.
Configuration options are coming from `DokkaBaseConfiguration`

```kotlin
dokka {
    html {
        customAssets.from(file("..."))                              // ConfigurableFileCollection
        customStyleSheets.from(file("..."))                         // ConfigurableFileCollection
        templatesDirectory.set(file("..."))                         // DirectoryProperty
        separateInheritedMembers.set(true)                          // Property<Boolean>
        mergeImplicitExpectActualDeclaration.set(true)              // Property<Boolean>
        footerMessage.set("message")                                // Property<String>
        homepageLink.set("https://www.github.com/owner/repository") // Property<String>
    }
}
```

# 6. other(custom) formats

There are multiple options depending on how much of an API we would like to provide.
Some of the options can be built upon each other and introduced in several steps.

## 6.1 adding plugin which will override an HTML format

The same task (`dokkaBuild`) is used to run Dokka.

```kotlin
dependencies {
    dokkaPlugin(libs.dokka.javadoc)
}
```

Pros:

* simple configuration, same as for plugins
* no additional API

Cons:

* the same task to execute dokka (`dokkaBuild`) will mean different things in different projects
* no easy way two build dokka in different formats

## 6.1 custom task for another format

```kotlin
// build.gradle.kts with applied dokka plugin

tasks.regsiter<DokkaTask>("dokkaBuildJavadoc") {
    plugins.add(libs.dokka.javadoc)
    pluginConfigurations.register("plugin.class.name") {} // optional, if format needs configuration
    outputDirectory.set(file("build/dokkaJavadoc"))
    // list of properties should look similar to `dokka` extension configuration and by default inherits them
}
```

Pros:

* almost no API changes, as `DokkaTask` should be exposed anyway for `dokkaBuild`
* it's possible to build multiple Dokka outputs (even multiple HTML outputs)

Cons:

* not very discoverable API
* could be bad with multi-module projects as it could require manual cross-project dependency management and one task
  could not be enough
* Current API could be problematic with Gradle confiugration-cache, and may require additional manual setup for
  dependencies to resolve Gradle configurations (`plugins` property)

## 6.3 custom DSL for another format (Oleg's favorite)

> Other DSLs are possible, but the idea is the same — will be explored additionally if needed

```kotlin
dokka {
    format(libs.dokka.javadoc, name = "javadoc") {
        property("something", "something")
        outputDirectory.set("...")
    }
}
```

Pros:

* easy to add additional format support
* no tasks configuration, only extensions

Cons:

* New API to support even if there are no use-cases
* additional API/ABI stability

Theoretically, can be based on implementation of option 6.2.

## 6.4 custom DSL only for some out-of-the box formats (like javadoc)

Instead of full blown DSL for custom formats we could provide option 6.1/6.2 + small DSL for some formats:

```kotlin
dokka {
    javadoc {
        enabled.set(true)
        outputDirectory.set(file("build/dokkaJavadoc"))
        // no other options here
    }
}
```

# TODO

* Workarounds for configuration issues in real projects
* absolute/relative paths
* 