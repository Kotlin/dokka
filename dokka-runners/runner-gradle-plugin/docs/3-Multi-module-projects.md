# Dokka Gradle Plugin DSL 2.0 - multi module projects

# 1. Applying Dokka Gradle Plugin (DGP)

Note: here we only describe how DGP can be added to project - aggregation and configuration will be discussed later in
doc and is out of the scope of the first paragraph.

Note: Here and in other places, we describe two possibilities: with and without a settings plugin.
Using settings plugin is an additional configuration which is based on project plugins, but have less restrictions
regarding Isolated Projects.

Note: the best default for aggregation will be to make it automatically include all projects in the multi-module result

Requirements:

* DGP of the same version should be applied to all modules to make it work correctly

## 1.1 Using only DGP for projects

### 1.1.1 explicitly in projects

For every project that needs dokka + root (aggregate) apply plugin:

```kotlin
// subproject/build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}
```

Cons:

* a lot of manual work to add this to every project
* harder to keep the version in sync (version catalogs could help)
* easy to forget to add plugin to new modules

### 1.1.2 via convention plugins

Create a convention plugin where dokka is applied and apply convention plugin where needed:

```kotlin
// buildSrc/src/main/kotlin/dokka-convention.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}
```

Pros:

* suggested by Gradle
* if there is something else related to dokka/documentation, it will be in one place

Cons:

* hard to do it if there is no buildSrc/build-logic setup already there

Additionally need to add dokka to classpath in `buildSrc/build-logic`:

```kotlin
// buildSrc/build.gradle.kts
dependencies {
    implementation(libs.dokkaGradlePlugin)
    // or
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
}
```

### 1.1.3 in root project via `subprojects/allprojects`

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

subprojects {
    plugins.apply(id = "org.jetbrains.dokka")

    // or if we want to apply to projects where kotlin is installed
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        plugins.apply(id = "org.jetbrains.dokka")
    }

    // of if we want to apply to when any kotlin plugin is applied
    listOf(
        "org.jetbrains.kotlin.multiplatform",
        "org.jetbrains.kotlin.jvm",
        "org.jetbrains.kotlin.android",
    ).forEach { kotlinPluginId ->
        plugins.withId(kotlinPluginId) {
            plugins.apply(id = "org.jetbrains.dokka")
        }
    }
}
```

Pros:

* dokka is applied to all needed projects in one file/place
* trivial to understand what's going on here when need to apply to all projects

Cons:

* not compatible with Isolated Projects
* rootProject is used not only for dokka and so could be huge already
* `plugins` block should be at the beginning of the file, but applying plugin to subprojects can be anywhere in the
  file (this can be confusing)
* filtering by Kotlin plugin is hard

### 1.1.4 in the root project via custom Dokka DSL (or may be some new shiny Gradle API)

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    aggregation {
        // or similar DSL
        applyPluginToSubprojects()
        // where `kotlin-multiplatform` plugin is applied
        applyPluginToKotlinMultiplatformProjects()
        // could be default configuration
        // or any kotlin plugins
        applyPluginToKotlinProjects()
    }
}
```

Pros:

* the same pros as in 1.4 but simplier regarding filtering

Cons:

* at the current moment, there is no possibility to do this in Gradle, which is compatible with Isolated
  Projects: https://github.com/gradle/gradle/issues/22514

## 1.2 Using DGP for settings

In root `settings.gradle.kts` (not in `build.gradle.kts`)

```kotlin
// settings.gradle.kts
plugins {
    id("org.jetbrains.dokka")
    // or another id - depends on what we want/can
    id("org.jetbrains.dokka.settings")
}

dokka {
    // or similar DSL
    applyPluginToSubprojects()

    // where `kotlin-multiplatform` plugin is applied
    applyPluginToKotlinMultiplatformProjects()
    // 2 options below could be the default configuration
    applyPluginToKotlinProjects()
    applyPluginToRootProject()
}
```

Pros:

* it should work fine with any kind of projects (even single-module ones)
* it should work fine with Isolated Projects
* AGP also provides settings plugin in recent versions

Cons:

* settings plugins are not a very popular thing
* additional learning curve as mostly users use `build.gradle.kts` and not `settings.gradle.kts`

# 2. Aggregation setup

Requirements:

* it allows setting up aggregation build in a root project (the most popular workflow)
* it allows setting up aggregation build in some project (useful when dokka is just a part of the setup for docs)
* should work with Gradle restrictions

## 2.1 Using only DGP for projects

### 2.1.1 explicit gradle constructs

```kotlin
// root/aggregate build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dependencies {
    dokkaAggregate(":subproject1")
    dokkaAggregate(":subproject2")
    // and so on all projects

    // or 
    subprojects.all {
        // we can filter here
        if (name != "some-internal-module") {
            dokkaAggregate(path)
        }
    }
}
```

Notes:

* dokkaAggregate is a Gradle configuration that can be configured to work in two ways:
    * fail if a provided project has no dokka applied
      Pros: allows understanding that the build is configured wrong
      Cons: complicates setup of applying Dokka plugin to where it's needed
    * ignore if provided project has no dokka applied
      Pros: it's possible to just use `subprojects.all { dokkaAggregate(path)` and it will work in all cases
      Cons: if some project is missing `dokka` for some reason we can understand this only by checking final
      documentation
    * may be it's possible to do something else here
* this `dokkaAggregate` behavior will still be there even if we hide it under our API as it's the only way to work with
  shared data with Isolated Projects restrictions

Pros:

* default Gradle constructs, no additional API

Cons:

* not possible to filter projects by applied plugins in Isolated Projects requirements

### 2.1.2 custom dokka DSL

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    aggregation {
        // will include all subprojects except `exclude` ones 
        includeSubprojects(exlcude = listOf(":some-internal-module"))
        // or more natural
        includeSubprojectsExcluding(exlcude = listOf(":some-internal-module"))

        // include just one project
        includeProject(":explicit:path:to:project")
    }
}
```

Pros:

* easier to use as we hide some Gradle constructs
* we can combine this API with the plugin applying, but we still need to be able to do it separately as applying plugins
  can be done via other ways

Cons:

* if subproject doesn't have `dokka` applied we need to explicitly filter it (not possible to do otherwise to be
  compatible with Isolated Projects)

### 2.2 dokka DSL in settings.gradle.kts

```kotlin
// settings.gradle.kts
plugins {
    id("org.jetbrains.dokka")
    // or another id - depends on what we want/can
    id("org.jetbrains.dokka.settings")
}

dokka {
    // by default we can create an aggregate in root project
    // DSL could be a little different, like may be `rootAggregation` but the idea will be the same
    aggregation {
        // similar DSL to the one above for case of `build.gradle.kts`
        includeProjects(exclude = ":some-internal-module")
        // or
        includeProjectsExcluding(exclude = ":some-internal-module")
    }
    // aggregate in some other place, f.e in `:docs` project
    aggregation(":docs") { /*...*/ }

    // OR we could have another name
    multiModule { /*...*/ }
    multiModule(":docs") { /*...*/ }

    // and for collector use case
    collector { /*...*/ }
    collector(":docs") { /*...*/ }
}
```

Pros:

* it most likely works with Gradle Isolated Projects

Cons:

* same cons as before for settings plugins

# 3. Running Dokka Gradle Plugin tasks

## Option 1: single task for both multi-module and single-module

F.e `./gradlew dokkaBuild`

Pros:

* one task for all kind of projects
* easy to document and use

Cons:

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

## Option 2: separate task for multi-module and single-module

F.e:

* single-module: `./gradlew dokkaBuild`
* multi-module: `./gradlew dokkaBuildAggregated`

Pros:

* tasks are somehow decoupled and provide more flexibility during implementation

Cons:

* different task names
* harder to understand which task should be run
* scales bad regarding custom formats

# 4. Sharing configuration

## 4.1 Using only DGP for projects

### 4.1.1 explicitly in projects (no sharing)

Copy-paste configuration in each project.

### 4.1.2 via convention plugins

Convention plugin with configuration:

```kotlin
// buildSrc/src/main/kotlin/dokka-convention.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    // same configuration as in single-module setup
    includeEmptyPackages.set(true)
}
```

Pros:

* Gradle suggests to share configuration like this

Cons:

* if the project don't have convention plugins - needs to setup them just for dokka

### 4.1.3 in root project via custom DSL

```kotlin
// root build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    includeEmptyPackages.set(true)
    aggregation {
        // subproject included for aggregation will use what is configured here 
        inheritConfiguration.set(true)
    }
}
```

Pros:

* one boolean flag

Cons:

* it could be not easy to understand what is shared and what is not
* most likely it's not compatible with Isolated Projects in mind (maybe not possible right now)

### 4.1.4 custom DSL in another way

```kotlin
// in some subproject build.gradle.kts or in convention plugin
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    inheritConfigurationFrom(rootProject)
}
```

Pros:

* should be compatible with Isolated Projects

Cons:

* it still needs to have some configuration for every project

### 4.1.4 in root project via `subprojects/allprojects`

```kotlin
// root build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    // configuration for root projects
}

subprojects {
    // plugin should be also applied here, f.e plugins.apply(id = "org.jetbrains.dokka") or via some DSL described above
    dokka {
        // configuration for subprojects
        includeEmptyPackages.set(true)
    }
}
```

Pros:

* No additional DSL

Cons:

* not compatible with Isolated Projects (accessing extension of another project)
* confusion of which configuration should be in root and which should be in `subprojects` block
* `subprojects` vs `allrpojects` confusion
* could be an issue that `dokka` is not accessible if plugin to subprojects is applied somewhere after this block

## 4.2 Using DGP for settings

```kotlin
// settings.gradle.kts
plugins {
    id("org.jetbrains.dokka")
    // or another id - depends on what we want/can
    id("org.jetbrains.dokka.settings")
}

dokka {
    // will be applied to all projects in build
    includeEmptyPackages.set(true)

    // matching by `path` (optional)
    // (it may be possible to enhance and provide more information, depends on Gradle)
    perModule("*internal*") {
        suppress.set(true)
    }
    // or
    perModule("*-data") {
        includeDeprecated.set(true)
    }
}
```

Pros:

* for simple cases, all configuration is one place—one file

Cons:

* with something like `perModule` we abuse configuring of specific projects in `settings`, instead of configuring them
  in projects.
  This is not conventional

# 5. Some questions

## Question 1

Aggregation project ambiguity: `root` (or other aggregation project) can have both sources and aggregation, TBD what
to do here, but looks like it

Project:

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

* Should dokka generate documentation for two (subproject-a + subproject-b) or three (additionally for root) modules?
* Should we support such cases or just throw?
* What other possibilities do we have?
* what about tasks?

# 6. HTML format specifics

Configuration should be the same for both sub-projects and aggregate-project

One of the possible ways to do it with DGP for projects

```kotlin
// root build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

// simplest case
// `allrpojects` and not `subproject` as we need to also configure `root` with the same values  
allrpojects {
    plugins.apply("org.jetbrains.dokka")
    dokka {
        html {
            customStyleSheets.from(rootDir.resolve("folder"))
        }
    }
}
```

DGP for settings:

```kotlin
// settings.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    html {
        customStyleSheets.from(rootDir.resolve("folder"))
    }

    aggregation { /*...*/ }
}
```

Notes:

* `html` here will be applied to all modules where dokka applied (aggregation including)
* `rootDir` could be confusing here, as it point to a directory where `settings` located and not of a project where it's
  configured

# 7. What if KGP apply dokka by default (or via some flag) everywhere if there is some `koltin` project

* In this case we will need to apply dokka only to root project
* If KGP will have settings plugin it will be able to apply Dokka even to root project
* In this case we can assume that dokka exists everywhere and so we don't need to care about issues with configurations.
  Because of it, we will not need to define projects which are included in aggregation but only exclusions
*

# 8. Assumptions for examples

* we are able to include all projects with Dokka plugin in aggregation automatically
  via `subprojects { dokkaAggregate(this) }` in a Gradle compatible way
