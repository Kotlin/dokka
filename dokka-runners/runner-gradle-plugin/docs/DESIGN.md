# Dokka Gradle Plugin DSL 2.0

## Shortcuts

* DGP = Dokka Gradle Plugin (runner)
* DE = Dokka Engine (analysis)
* DEP = Dokka Engine Plugin (analysis plugins)

## 1\. Apply Dokka to a single-module project

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
    // or if DGP will be near KGP we can change ID to `org.jetbrains.kotlin.dokka` and so use gradle shortcuts:
    kotlin("dokka")
}
```

## 2\. Apply Dokka to a multi-module project

As Gradle provides multiple ways to do it, it will be possible to do it in different ways:

1. explicitly in projects or via convention plugins (recommended for advanced users)
2. via `subprojects` or via custom DSL from Dokka (recommended for simple cases)
    * could be not compatible with Isolated Projects
    * could be not needed if KGP can apply Dokka by default in some way (f.e via flag in KGP settings plugin)

### Using KGP integration in settings.gradle.kts

If KGP will have settings plugin we can add property there to apply DGP to all projects with Kotlin plugin + `rootProject` for aggregation.

```kotlin
// settings.gradle.kts
plugins {
  // KGP for settings possible name
  kotlin("settings")
}

kotlin {
  // will cause applying Dokka to all projects with `kotlin` plugins (jvm, android, multiplatform)
  applyDokka.set(true)
}
```

### Use DGP DSL

```kotlin
// ROOT build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    aggregation {
        applyPluginToIncludedProjects.set(true)
    }
}
```

`applyPluginToIncludedProjects` is just a boolean, as there is no need for other customizations,
it will cause applying Dokka plugin to all included in aggregation projects.
It's fine if it will be added to projects without KGP, as for such cases we will just skip all the logic of DGP
(no tasks will be available)

### Using out-of-the-box functionality (advanced cases)

Applying explicitly or in convention plugins works in the same as with single-module projects:

```kotlin
// subproject/build.gradle.kts or buildSrc/src/main/kotlin/dokka-convention.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}
```

Applying via `subprojects` is still possible, but not recommended, better to use DGP DSL:

```kotlin
// ROOT build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

subprojects {
    plugins.apply(id = "org.jetbrains.dokka")
}
```

## 3\. Aggregation in a multi-module project

By default, without additional configuration, if Dokka is applied in root projects,
all subprojects with applied Dokka plugin will be included in the documentation,
so in the following example it's full required setup for Dokka in multi-module projects.

```kotlin
// ROOT build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    aggregation {
        applyPluginToIncludedProjects.set(true)
    }
}
```

Additionally, it should be possible to easily exclude projects from aggregation, in case some modules are included
automatically (because Dokka is applied everywhere):

```kotlin
// ROOT build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    aggregation {
        // allprojects is enabled by default if applied in root project  
        includeAllprojects {
            // via project name
            exclude("subproject-name")
            // or via project path
            exclude(":subproject:name")
            // both variants supports wildcards
            // so this should be possible
            exclude(":tests:*")
            // vararg and lists shoud be supported
            exclude("name1", "name2")
        }
    }
}
```

Advanced: if aggregation is needed to set up in a separate project, aggregated projects should be specified explicitly:

```kotlin
// documentation/build.gradle.kts - NOT IN ROOT
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    aggregation {
        includeAllprojects()
        // or
        includeSubprojects {
            // optional exclusions, same as in above example
            exclude("name")
            // or
            exclude(":subproject:path")
        }
        // or names/paths
        includeProjects("subproject-name", ":subproject:path")
    }
}
```

Advanced++: under the hood we will still use Gradle configurations, so something like this will be still possible:

```kotlin
// ROOT build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dependencides {
    dokkaAggregation(project(":subproject:a"))
    dokkaAggregation(project(":subproject-b"))
}
```

### How can we distinguish if we are building multi-module or single-module documentation?

If DGP is applied in the root project, it can mean two things:

1. if there is KGP applied -\> it's a single-module project, aggregation is not configured
2. if there is no KGP applied -\> it's a multi-module project, and so we need to automatically configure default
   aggregation which will include all subprojects by default.

## 4\. Execute Dokka tasks

Out-of-the-box without any additional configuration the task will build Dokka HTML documentation in `build/dokka`

`./gradlew :dokkaBuild` - single task to launch Dokka Engine for both single and multi-module projects

Note: all user-facing tasks should be prefixed with `dokka`

Note regarding multi-module projects:
If `dokkaBuild` task will have the same name for both single-module and multi-module setups,
then calling just `dokkaBuild` in Gradle will call `dokkaBuild` in both root and submodules.
Calling `:dokkaBuild` (prefixed with `:`) is what will call only the aggregated task.
Calling `:dokkaBuild` (prefixed with `:`) in single-module projects will also work.
The main issue here is that to build aggregated(multi-module) result we can't reuse results of `dokkaBuild` from
submodules because `dokka` needs different kind of artifact built with `delayTemplateSubstitution=true`.
And so when calling just `dokkaBuild` in multi-module tasks will cause to run following tasks
(task names are subject to change):

* `:submodule1:prepareDokkaModule` - used by root `dokkaBuild` task
* `:submodule2:prepareDokkaModule` - used by root `dokkaBuild` task
* `:submodule1:dokkaBuild` - not required to build multi-module, doesn't depend on `prepareDokkaModule`
* `:submodule2:dokkaBuild` - not required to build multi-module, doesn't depend on `prepareDokkaModule`
* `:dokkaBuild`

if running `:dokkaBuild` (prefixed with `:`):

* `:submodule1:prepareDokkaModule`
* `:submodule2:prepareDokkaModule`
* `:dokkaBuild`

So there are some technical limitations here, but they are not blocking, and may be there is a workaround.

## 5\. Dokka execution configuration

All available properties in root of DGP DSL:

```kotlin
dokka {
    // global properties
    dokkaEngineVersion.set("1.9.20")
    offlineMode.set(false)
    warningsAsErrors.set(false)

    // module level properties
    suppressObviousFunctions.set(false)
    suppressInheritedMembers.set(false)

    // sourceSet level properties
    includeEmptyPackages.set(true)

    // `includedDocumentation` uses md files in `module documentation file format`
    includedDocumentation.from("includes.md") // ConfigurableFileCollection

    // package level properties
    documentedVisibilities.set(setOf(PUBLIC, INTERNAL))
    warnOnUndocumented.set(true)
    includeDeprecated.set(false)
}
```

In the case of a multi-module project, all those properties will be shared to all projects included in aggregation
and current project itself.
In case it's applied in ROOT project with default configuration, configuration will be shared to all subprojects.
Still, there are some properties which affect generation of HTML in a current project only,
and so they should be configured only for a specific project.

```kotlin
dokka {
    // those properties are additional to the one above and affect only current project
    currentProject {
        moduleName.set(project.name)
        moduleVersion.set(project.version.toString())
        outputDirectory.set(file("build/dokka"))
    }
}
```

### SourceSet configuration

```kotlin
dokka {
    // configure sourceSets matching a pattern
    perSourceSet("*Main") {
        suppress.set(false)

        // the properties below will override the same properties declared in `root` configuration

        // sourceSet level properties 
        includeEmptyPackages.set(true)

        // package level properties
        documentedVisibilities.set(setOf(PUBLIC, INTERNAL))
        warnOnUndocumented.set(true)
        includeDeprecated.set(false)
    }
}
```

SourceSet configuration is shared in the same way as ordinary properties.
If there is a need to create custom sourceSets, change classpath or change some other project-specific property,
it's possible to access `sourceSets` as `DomainObjectCollection` inside `currentProject`:

```kotlin
dokka {
    currentProject {
        // `sourceSets` is available only inside `currentProject`
        sourceSets.named("commonMain") {
            // here is a list of additional properties available inside currentProject

            // those will be set by DGP from information retrieved from KGP
            displayName.set(name)
            platform.set(KotlinPlatformType.jvm)
            jdkVersion.set(8) // TODO: only used for jvm and android - may be it's not needed at all  
            languageVersion.set(KotlinVersion.KOTLIN_2_1) // or just string?
            apiVersion.set(KotlinVersion.KOTLIN_2_1)

            classpath.from(file("libs/dependency.jar")) // ConfigurableFileCollection
            sourceFiles.from(file("src")) // ConfigurableFileCollection
            suppressedSourceFiles.from(file("build/generated")) // ConfigurableFileCollection
            samples.from("samples/Basic.kt", "samples/Advanced.kt") // ConfigurableFileCollection

            // all other properties configured above in `perSourceSet` are also available.
        }
    }
}
```

### perPackage configuration

`perPackage` configuration is very similar to `perSourceSet`:

```kotlin
dokka {
    // same patterns as in `perSourceSet`
    perPackage("*.internal.*") {
        // package specific properties
        suppress.set(false)

        // the properties below will override the same properties declared in `root` or `sourceSet` configuration

        // package level properties
        documentedVisibilities.set(setOf(PUBLIC, INTERNAL))
        warnOnUndocumented.set(true)
        includeDeprecated.set(false)
    }

    // can be aslo configured for sourceSet (via any configuration option)
    perSourceSet("macos*") {
        perPackage("*internal*") { /* same options available as above */ }
    }
}
```

Package configuration is shared and can be configured for a current project only in the same way as ordinary properties.

```kotlin
dokka {
    currentProject {
        perPackage("*.internal.*") { /*...*/ }
    }
}
```

### SourceLink configuration

```kotlin
dokka {
    sourceLink("https://www.github.com/owner/repository/tree/main")
    // or if additional properties should be configures
    sourceLink("https://www.github.com/owner/repository/tree/main") {
        remoteLineSuffix.set("#L")
    }
    // or without shortcut for URL
    sourceLink {
        remoteUrl.set("https://www.github.com/owner/repository/tree/main")
        localDirectory.set(project.rootDir)
        remoteLineSuffix.set("#L")
    }

    // multiple source links could be configured if needed
    sourceLink {
        remoteUrl.set("https://www.github.com/owner/REPO_PUBLIC/tree/main")
        localDirectory.set(file("publicSources"))
    }
    sourceLink {
        remoteUrl.set("https://www.github.com/owner/REPO_PRIVATE/tree/main")
        localDirectory.set(file("privateSources"))
    }
}
```

By default `localDirectory` points to the root of the project,
so that f.e declaring remoteUrl to the root of the GitHub project URL is enough.
Additionally, we can also add validation for this URL,
at least for known VCS (GitHub, etc.) so that it at least points to something logical like:
`http(s)://www.github.com/{OWNER}/{REPO}/tree/{SOMETHING}`.

`localDirectory` can be both absolute/relative path, if relative it's resolved based on a root project path like in
`file("...")`.

### ExternalDocumentationLink configuration

```kotlin
dokka {
    documentationLinks {
        // the simplest case when no package-list url provided, and it's inferred from url
        externalLink("https://kotlinlang.org/api/kotlinx.coroutines")
        // remote package list url is specified explicitly
        externalLink("https://kotlinlang.org/api/kotlinx.coroutines") {
            packageListUrl("https://kotlinlang.org/api/kotlinx.coroutines/somewhere/package-list")
        }
        // local package list file
        externalDocumentationLinkFrom("https://kotlinlang.org/api/kotlinx.coroutines") {
            packageListFile("build/downloaded/package-list")
        }

        // some predefined useful links, can be enabled or disabled (enabled by default)
        linkToKotlinStdlib(enabled = false)

        // this could be useful also (disabled by default)
        linkToKotlinxCoroutines()

        // if, for some reason, some of the link becomes outdated it can be fixed by lambda
        linkToKotlinxSerialization {
            // via remote url
            packageListUrl("kotlinlang.org/api/kotlinx.serialization/somewhere-new/package-list")
            // or via local file
            packageListFile(file("build/downloaded/package-list"))
        }

        // fully custom
        externalLink {
            removeUrl = "kotlinlang.org/api/kotlinx.coroutines"
            packageListUrl("kotlinlang.org/api/kotlinx.serialization/somewhere-new/package-list")
            // or
            packageListFile("build/downloaded/package-list")
            // or
            packageListLocation = "kotlinlang.org/api/kotlinx.serialization/somewhere-new/package-list".toURI()
            // or
            packageListLocation = file("build/downloaded/package-list").toURI()
        }
    }
}
```

External documentation links can be also configured on `SourceSet` level.

### Inheritance of configuration

```kotlin
dokka {
    documentedVisibilities.set(setOf(DokkaDeclarationVisibility.PUBLIC))

    perPackage("org.internal") {
        // overrides what was set in `root` for this package
        documentedVisibilities.set(setOf(DokkaDeclarationVisibility.INTERNAL))
    }

    perSourceSet("jvmMain") {
        // documentedVisibilities is a `Set`, value will be inherited from `root`
        // so if we use `add` it means that we will inherit and update
        // to override value, we will need to use `set`;
        // here in the end for `jvmMain` documentedVisibilities will contain PUBLIC+PRIVATE
        documentedVisibilities.add(DokkaDeclarationVisibility.PRIVATE)

        // fully overrides what was set in `root`
        documentedVisibilities.set(setOf(DokkaDeclarationVisibility.PRIVATE))

        perPackage("org.internal") {
            // overrides what was set in `sourceSet` and `root` for this package in this sourceSet
            documentedVisibilities.set(emptySet())
        }
    }
}
```

## 6\. Dokka HTML configuration

HTML format is single format enabled by default, HTML format configuration is accessible under `dokka.html` sub-DSL.
Configuration options are coming from `DokkaBaseConfiguration`.

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

Configuration is shared and can be configured for a current project only in the same way as ordinary properties:

```kotlin
dokka {
    currentProject {
        html { /*...*/ }
    }
}
```

## 7\. Dokka Engine Plugins

Different Dokka Engine Plugins may or may not have configuration so both cases should be considered.
Here are how different options will work for both cases.
We should support adding plugin via `group:artifact:version` String notation and via Gradle Version Catalogs.

To just add plugin without configuration:

```kotlin
dokka {
    plugin("group:artifact:version")
    // or
    plugin(libs.dokka.kotlinAsJava)
}
```

Note: both official plugins (like kotlinAsJava) and external ones are applied in the same way.

To add a plugin and configure it, we need to provide the class name of plugin and its configuration

```kotlin
dokka {
    plugin(libs.dokka.mermaidPlugin, "com.glureau.HtmlMermaidDokkaPlugin") {
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
}
```

It's also possible to add a plugin and configure it in separate calls to be able to separate applying plugins and
configuration:

```kotlin
dokka {
    // apply dokka plugin
    plugin(libs.dokka.mermaidPlugin)

    // configure it (possible somewhere else)
    pluginConfiguration("com.glureau.HtmlMermaidDokkaPlugin") { /*...*/ }

    // it's also possible to call it multiple times for the same class name
    pluginConfiguration("com.glureau.HtmlMermaidDokkaPlugin") { /*...*/ }
}
```

Technical note: function name `property(name, value)` can clash with `Project.property(name)` which is implicitly
imported in Gradle scripts:

```kotlin
dokka {
    pluginConfiguration("com.glureau.HtmlMermaidDokkaPlugin") {
        property("L") // will compile and resolve to `project.property("L")`
    }
}
```

replacement could be:

* `pluginProperty`
* `intProperty`/`longProperty`/etc.
* `parameter`/`param`

Applying plugins works in the same way as with other configuration options, so decclared in root will be shared to
included subprojects.
To configure only current project same DSL in `currentProject` is available:

```kotlin
dokka {
    currentProject {
        plugin(libs.dokka.versioning)
    }
}
```

## 8\. Dokka Engine Formats (Variants)

This API should be used only by advanced users and so should be annotated with some OptIn annotation with ERROR.

Dokka Formats are just Dokka Engine Plugins.
So there is a requirement to build javadoc instead of html, just applying plugin will be enough:

```kotlin
dokka {
    plugin(libs.dokka.javadoc)
}
```

If there is a requirement to build documentation in several formats, it's possible to create a new variant for this.
Creating new variants not only allows overriding the format (like replacing html with javadoc).
It also allows building separate HTML outputs, f.e with different branding or other configurations
(like one with only public declarations, another with internal declarations and may be another one excluding sourceSet).
This will also allow building documentation for multiple Android flavours (f.e paid and free).

Here is an example for javadoc:

```kotlin
dokka {
    variants.register("javadoc") {
        plugin(libs.dokka.javadoc)
        currentProject {
            outputDirectory.set(dir("build/javadoc"))
        }
    }
}
```

Only adding plugin and changing output directory is needed.
But overall, it should expose all the same properties which are available in the root DSL
(except for `variants` itself).
All configuration options defined in root DSL will be shared to variant.
`variants` by default will also contain `main` variant,
which is the default one and used by default tasks like `dokkaBuild`.
So if there is a need to configure two formats differently, this should be possible:

```kotlin
dokka {
    // will be shared
    html {
        footerMessage.set("something")
    }
    variants {
        named("main") {
            documentedVisibilities.set(setOf(PUBLIC))
        }
        named("internal") {
            documentedVisibilities.set(setOf(INTERNAL))
        }
    }
}
```

Gradle configuration/tasks for variants (except for `main`) should have suffix `variant.name`, f.e: `dokkaBuildJavadoc`
in case `variant.name=javadoc`.

### Technical note:

Theoretically, we can expose two DGP ids:

* `org.jetbrains.dokka.base` - which will create no variants, apply no defaults for aggregation
* `org.jetbrains.dokka` - which will create `main` variant and if it's root project will include subprojects for
  aggregation

This could also simplify the overall internal structure of the plugin.

## 9\. Future work/improvements (not required for initial release)

These are optional things which could or could not improve the experience usage of Dokka Gradle Plugin.

### JavaDoc JAR generation

The main use case for building jar is publishing `javadoc.jar` to Maven Central, because of its requirements.
The other one is if it's closed source and the only thing that could be provided is `javadoc.jar` as there is
no `sources.jar`.
In this case, without any specific configuration provided by DGP, it could be achieved by:

```kotlin
plugins {
    id("org.jetbrains.dokka")
}

val javadocJar by tasks.registering(Jar::class) {
    // or tasks.named("buildDokka") if for some reason Gradle failed to generate accessors
    // or tasks.named("buildDokkaJavadoc") if custom variant is needed
    from(tasks.buildDokka)

    archiveClassifier.set("javadoc")
}

// optionally: add `javadoc.jar` to publications (config may be different for more complex projects) 
publishing.publications.withType<MavenPublication>().configureEach { artifact(javadocJar) }
```

We could improve this by introducing some kind of helper function, but it heavily depends on do we want to provide this,
(IMO, we don't), but f.e:

```kotlin
// just this for simple cases
val dokkaJar = dokka.registerDokkaJarTask()
// or with additional setup
val dokkaJar = dokka.registerDokkaJarTask(
    classifier = "html", // optional, default could be `javadoc` or `dokka`
    variant = "release", // optional, default is `main`
) { // this: Jar - optional, additional configuration of `Jar` task
    destinationDirectory.set(file("build/dokkaJavadoc"))
}

// optionally: add `javadoc.jar` to publications manually (config may be different for more complex projects) 
publishing.publications.withType<MavenPublication>().configureEach {
    artifact(dokkaJar)
}
```

Additionally, to this, we could provide extensions for KGP to be able to include `javadoc.jar` automatically:

```kotlin
dokka {
    // some dokka configuration
}

kotlin {
    // just a single call is enough (similar to `withSourcesJar()`
    withDokkaJar()
    // or if we need additional configuration
    withDokkaJar(
        classifier = "javadoc", // optional, default could be `javadoc` or `dokka`
        variant = "main" // optional, default is `main`
    )
}
```

### Settings Plugin

Settings plugin could be an option for simplifying applying DGP to all subprojects, as well as sharing configuration.
DGP will be applied to all projects where `KGP` is applied + root project (for aggregation).
Though, it's not that popular and can bring even more confusion on how to configure DGP.

```kotlin
// settings.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    // all the same properties as in project DSL, except for `currentProject`

    // additionally, it's possible to configure something per module by name or path
    perModuleName("test*") { /*...*/ }
    // or
    perModulePath(":test:*") { /*...*/ }

    // configures aggregation for the root project
    aggregation {
        // the same dsl as for project plugin
        // `includeAllProjects` is applied by default
        includeAllProjects()
    }
}
```

If/when KGP will provide settings plugin the combination of DGP and KGP could look strange:

```kotlin
// settings.gradle.kts
plugins {
  kotlin("settings")
  kotlin("dokka")
}

kotlin {
  // applying dokka goes here
  // we could make this optional, but still, the property will be available
  applyDokka.set(true)
}

dokka {
  // shared configuration goes here
}
```

In this case, we could more tightly integration DGP into KGP and so merge DSLs:

```kotlin
plugins {
  kotlin("settings")
}

kotlin {
    // by default `enabled=true`
    dokka(enabled = true) {
      // shared configuration
    }
}
```

### Custom properties

We can provide additional `unsafe` compatibility properties configuration for cases like:

* in DGP 2.1.0 we had some deprecated property in DSL,
  in DGP 2.2.0 we removed this property,
  but we still want to execute Dokka (DE) 2.0.0 (because of some bug) and we need this property there.
* We can also hide some `advanced`/EAP properties in this way, f.e enabledAllTypesPage,
  as we don't want if it is stabilized, so we don't want to update DSL for this.

API should be marked with some `DelicateDokkaGradlePluginApi` opt-in.

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
    perSourceSet("main*") {
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

    // or for currentProject
    currentProject {
        customProperties {
            property("enabledAllTypesPage", true)
        }
        // and other nested scopes
    }

    // of for other blocks if required
}
```

## Notes:

Everywhere where there is `*` used in strings its wildcards.
Wildcards (glob-like) are used and not regex because Gradle also mostly uses wildcards.
They are easier to write and understand, regex adds too much of complexity.
Example of a problem with regex:
A lot of projects uses `perPackage` as `glob-like` (wildcards), not as regex.
So instead of something like `com\\.example\\.internal` they just write `com.example.internal`,
it works well (in all cases I think), as `.` in regex is just a match for any character.
Though, if we start matching with `*` the behavior could be different:

* `com.example.internal.*` - works acceptably the same for both glob and regex
* `com.example.internal*` - works differently (glob - fine, regex - match `l` character multiple times)
* `.*internal.*` - works fine in regex, will not work in glob (because of first symbol is `.`)
* `*internal*` - incorrect regex, works fine with glob