# Dokka Gradle Plugin DSL 2.0 - single module projects

DGP = Dokka Gradle Plugin
DE = Dokka Engine
DEP = Dokka Engine Plugin

# 1. Applying Dokka Gradle Plugin (DGP)

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

Notes:

* While for single-module projects, one task is enough, for multi-module some additional support tasks may be needed
*

# 3. Adding Dokka Engine Plugins

Different Dokka Engine Plugins may or may not have configuration so both cases should be considered.

Here are how different options will work for both cases.

## 3.1 Adding plugin WITHOUT configuration

Both options should support adding plugin via `group:artifact:version` String notation and via Gradle Version Catalogs.

Option 1: via Gradle configurations (as in dokkatoo):

```kotlin
dependencies {
    dokkaPlugin("dependency")
    // or
    dokkaPlugin(libs.dokka.kotlinAsJava)
}
```

Pros:

* simple construct, available out-of-the-box in Gradle when using configurations
  (generated type-safe `dokkaPlugin` accessor)

Cons:

* configuration and applying of plugins is split in different places (`dokka` extension and `dependencies` block)
* no type-safe accessor outside `build.gradle.kts`, f.e inside custom plugins / buildSrc / build-logic files

Example of usage in custom plugin

```kotlin
// some custom Gradle plugin in buildSrc
class SomePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // apply dokka plugin
        target.plugins.apply("org.jetbrains.dokka")

        dependencies {
            // no type-safe accessor
            "dokkaPlugin"("group:artifact:version")
        }
    }
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

Pros:

* adding plugins is via dokka own extension, no need to understand how Gradle works
* works fine with custom Gradle plugins

Cons:

* additional API/ABI to maintain
* plugin function argument can be of a lot of types (String, Provider, from version catalog), so or we need to use `Any`
  and document all possible values or create a lot of functions to mimic what is possible in Gradle
* less flexibility comparing to `dependencies` block because Gradle provides a lot of DSL there (f.e to include/exclude
  transitive dependencies or change some other settings regarding dependency resolution)

> Under the hood here we will do the same as calling `dokkaPlugin` in `dependencies`, so if some advanced usage is
> needed, first option API will be still possible

Example of usage in custom plugin:

```kotlin
// some custom Gradle plugin in buildSrc
class SomePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // apply dokka plugin
        target.plugins.apply("org.jetbrains.dokka")

        // standard Gradle DSL to configure extension
        target.extensions.configure<DokkaExtension>("dokka") {
            plugin("group:artifact:version")
        }
    }
}
```

## 3.2 Only configuring plugin (if it's added somewhere else)

We should be careful with such blocks as it could be error-prone because users might think that configuring plugin will
be enough to make it work.
Better to have the ability and suggest to add and configure plugin in one go.
Configuring plugin without directly adding it can be considered as advanced use-case.

```kotlin
dokka {
    pluginConfiguration(className = "com.glureau.HtmlMermaidDokkaPlugin") {
        // property value can be of different types: String, Int, Provider, File, FileCollection, etc
        property("lightTheme", "forest")
        property("darkTheme", "dark")
        property("file", file(""))
        property("fileCollection", files("1", "2", "3"))
        property("provider", provider {
            "some string which is computed from other property"
        }.map {
            it + "; add changed"
        })
    }
    // or some other name
    configurePlugin(className = "com.glureau.HtmlMermaidDokkaPlugin") { ... }
}
```

Question 1: What we need to do if multiple `pluginConfiguration` blocks will be called for the same className? F.e:

```kotlin
dokka {
    pluginConfiguration(className = "com.glureau.HtmlMermaidDokkaPlugin") {
        property("x", "x")
        property("y", "y")
    }
    pluginConfiguration(className = "com.glureau.HtmlMermaidDokkaPlugin") {
        property("x", "other")
    }
}
```

options:

1. merge configurations, so second configuration will just update values
2. override configuration, so only second configuration will be used
3. provide both configurations from DGP to Dokka Engine and let engine decide (at the current moment it will just take
   first configuration)
4. throw an error? Not a good idea, configuration can come from another plugin

Question 2: Should we somehow detect if configuration is used/unused when running Dokka Engine?

Notes:

* To configure the plugin, we need only its name and properties
* JSON configuration like in old plugin is out of scope:
    * it is hard to make it work correctly when using providers
    * it is hard to make it work correctly with Gradle build cache (because of relative vs absolute paths)
    * it is hard to make it work correctly with Gradle up-to-date checks (because user will need to explicitly register
      input/outputs for values used in configuration)
* DSL of `property` is inspired by `dokkatoo`
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

## 3.3 Adding plugin WITH configuration

Option 1: via Gradle configurations (as in dokkatoo):

```kotlin
dependencies {
    dokkaPlugin(libs.dokka.kotlinAsJava)
}

dokka {
    pluginConfiguration(className = "com.glureau.HtmlMermaidDokkaPlugin") {
        property("lightTheme", "forest")
        property("darkTheme", "dark")
    }
    // or some other name
    configurePlugin(className = "com.glureau.HtmlMermaidDokkaPlugin") { ... }
}
```

or depending on how to add plugin:

```kotlin
dokka {
    plugin(libs.dokka.kotlinAsJava)
    pluginConfiguration(className = "com.glureau.HtmlMermaidDokkaPlugin") {
        property("lightTheme", "forest")
        property("darkTheme", "dark")
    }
    // or some other name
    configurePlugin(className = "com.glureau.HtmlMermaidDokkaPlugin") { ... }
}
```

Nothing new: just a combination of previous options

Option 2: via custom DSL (v1)

```kotlin
dokka {
    plugin(
        dependency = libs.dokka.kotlinAsJava,
        className = "com.glureau.HtmlMermaidDokkaPlugin",
    ) {
        property("lightTheme", "forest")
        property("darkTheme", "dark")
    }
    // or some other name
    applyPlugin(...) { ... }
}
```

Combines `plugin` and `pluginConfiguration` in one block

Pros:

* rather easy to use and discover
* if function name = `plugin`, one function for both just applying to applying+configuring

Cons:

* if we do not provide `pluginConfiguration`, there is no way to just configure plugin which is applied somewhere before

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

Pros:

* multiple plugins can be configured when coming from one dependency: possible in Dokka Engine, can be useful when
  someone develops Dokka Gradle Plugins just for their projects, f.e someone can write multiple plugins in separate
  Gradle module and add them as one dependency but multiple plugins (and so multiple configurations)

Cons:

* additional nesting
* DSL for simple cases is more verbose

Option 4: via custom DSL (v3) (Oleg's favorite)

Overall, it's the both Option 1 + Option 2 at the same time that fixes other issues

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

Pros:

* rather simple for simple cases (one plugin/configuration for one dependency)
* provides flexibility for complex cases (where multiple configuration needed)

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

# 4. configuration

## Root configuration

```kotlin
dokka {
    // global properties
    dokkaEngineVersion.set("1.9.20")
    offlineMode.set(false)
    warningsAsErrors.set(false)
    outputDirectory.set(file("build/dokka"))

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

* patterns - regex vs glob(wildcards)
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

## Inheritance of configuration

```kotlin
dokka {
    documentedVisibilities.set(setOf(DokkaDeclarationVisibility.PUBLIC))

    perPackage("org.internal") {
        // overrides what was set in `root`
        documentedVisibilities.set(setOf(DokkaDeclarationVisibility.INTERNAL))
    }

    sourceSets.named("jvmMain") {
        // documentedVisibilities is a `Set`, value will be inherited from `root`
        // so if we will use `add` it means that we will inherit and update
        // to override value, we will need to use `set`;
        // here in the end for `jvmMain` documentedVisibilities will contain PUBLIC+PRIVATE
        documentedVisibilities.add(DokkaDeclarationVisibility.PRIVATE)

        // fully overrides what was set in `root`
        documentedVisibilities.set(setOf(DokkaDeclarationVisibility.PRIVATE))

        perPackage("org.internal") {
            // overrides what was set in `sourceSet` and `root`
            documentedVisibilities.set(emptySet())
        }
    }
}
```

## Custom Configuration properties

We can provide additional `unsafe` compatibility properties configuration for cases like:

* in DGP 2.1.0 we had some deprecated property in DSL,
  in DGP 2.2.0 we removed this property,
  but we still want to execute Dokka (DE) 2.0.0 (because of some bug) and we need this property there.
* We can also hide some `advanced`/EAP properties in this way, f.e enabledAllTypesPage,
  as we don't want if it is stabilized, so we don't want to update DSL for this.

```kotlin
dokka {
    // global/root configuration
    customProperties {
        // DSL is the same as for plugins configuration
        property("enabledAllTypesPage", true)
        // it can be structured
        property("nested") {
            property("someValue", true)
        }
    }

    // or for sourceSets
    sourceSets.configureEach {
        customProperties {
            property("hm", 1)
        }
    }

    // or for packages
    perPackage("org.internal") {
        customProperties {
            property("hm", 1)
        }
    }

    // of for other blocks if required
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
Some options can be built upon each other and introduced in several steps.

Note: we most likely need to somehow restrict/warn when using `javadoc` format with KMP projects.

## 6.1 adding plugin which will override an HTML format

The same task (`dokkaBuild`) is used to run Dokka.

```kotlin
// or via other DSL variants represented above
dokka {
    plugin(libs.dokka.javadoc)
}
```

Pros:

* simple configuration, same as for plugins
* no additional API

Cons:

* the same task to execute dokka (`dokkaBuild`) will mean different things in different projects
* building dokka in different formats at once is cumbersome
    * f.e if it's needed to generate both javadoc and kotlin html from mixed Kotlin/Java sources (String, Gradle, etc.)

## 6.2 custom task for another format

```kotlin
// build.gradle.kts with applied dokka plugin

tasks.regsiter<DokkaTask>("dokkaBuildJavadoc") {
    plugins.add(libs.dokka.javadoc)
    pluginConfigurations.register("plugin.class.name") {
        // optional, if format needs configuration
        property("something", "x")
    }
    outputDirectory.set(file("build/dokkaJavadoc"))
    // list of properties should look similar to `dokka` extension configuration and by default inherits them
}
```

Pros:

* almost no API changes, as `DokkaTask` should be exposed anyway for `dokkaBuild`
* it's possible to build multiple Dokka outputs (even multiple HTML outputs)

Cons:

* not very discoverable API
* it could be bad with multi-module projects as they may require manual cross-project dependency management and one task
  could not be enough
* Current API could be problematic with Gradle configuration-cache, and may require additional manual setup for
  dependencies to resolve Gradle configurations (`plugins` property)

## 6.3 function to create a custom task for another format

```kotlin
// build.gradle.kts with applied dokka plugin

val dokkaBuildJavadoc: TaskProvider<DokkaTask> = dokka.registerDokkaTask("dokkaBuildJavadoc") {
    // We can expose not `DokkaTask` here as receiver, 
    // but the same or simplified DSL which is used to configure `dokka` extension
    // by default it will inherit global/root configuration
    plugin(libs.dokka.javadoc)
    pluginConfiguration("plugin.class.name") {
        // optional, if format needs configuration
        property("something", "x")
    }
    outputDirectory.set(file("build/dokkaJavadoc"))
}
```

Pros:

* as we provide not `DokkaTask` as receiver but some other type, we could do more things under the hood,
  including working with configurations in configuration-cache friendly way
* could be easier regarding multi-module projects (same reason as above: another receiver)

Cons:

* additional API/ABI to maintain
* still could be bad with multi-module projects as they may require manual cross-project dependency management and one
  task

## 6.4 custom DSL for another format (Oleg's favorite)

> Other DSLs are possible, but the idea is the same — will be explored additionally if needed

```kotlin
dokka {
    // format will inherit all Dokka configuration defined in extension 
    format(libs.dokka.javadoc, name = "javadoc") {
        // same properties as when configuring plugins
        property("something", "something")

        // custom output directory
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

Theoretically, it can be based on the implementation of option 6.2/6.3.

## 6.5 custom DSL only for some out-of-the box formats (like javadoc)

Instead of full-blown DSL for custom formats, we could provide option 6.1/6.2 + small DSL for some formats:

```kotlin
dokka {
    javadoc {
        enabled.set(true)
        outputDirectory.set(file("build/dokkaJavadoc"))
        // no other options here as plugin has no configuration at the moment, but we could add something 
    }

    // or
    javadoc(enabled = true) {
        outputDirectory.set(file("build/dokkaJavadoc"))
    }

    // or if we know, that we will never have `javadoc` configuration options
    produceJavadoc(outputDirectory = file("build/dokkaJavadoc"))
}
```

## 6.6 custom function to create task for only for some out-of-the box formats (like javadoc)

It's mix of option 3 and 5:

```kotlin
dokka.registerDokkaJavadocTask("dokkaBuildJavadoc") {
    // only outputDirectory make sense, but possible to add possibility to override other properties from extension
    outputDirectory.set(file("build/dokkaJavadoc"))
}
// or
dokka.registerDokkaJavadocTask("dokkaBuildJavadoc", outputDirectory = file("build/dokkaJavadoc"))
```
