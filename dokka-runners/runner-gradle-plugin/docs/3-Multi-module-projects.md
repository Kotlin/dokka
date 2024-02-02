# Dokka Gradle Plugin DSL 2.0 - multi module projects

# 1. Applying Dokka Gradle Plugin (DGP)

Requirements:

* DGP of the same version should be applied to all modules to make it work correctly

## 1.1 explicitly in projects

For every project that needs dokka + root (aggregate) apply plugin:

```kotlin
// subproject/build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}
```

## 1.2 via convention plugins

Create a convention plugin where dokka is applied and apply convention plugin where needed:

```kotlin
// buildSrc/src/main/kotlin/dokka-convention.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}
```

Additionally need to add dokka to classpath in `buildSrc/build-logic`:

```kotlin
// buildSrc/build.gradle.kts
dependencies {
    implementation(libs.dokkaGradlePlugin)
    // or
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
}
```

## 1.3 in root project via `subprojects/allprojects`

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

subprojects {
    plugins.apply(id = "org.jetbrains.dokka")
}
```

## 1.4 in root project via custom Dokka DSL (or may be some new shiny Gradle API)

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    aggregation {
        // or similar DSL
        applyPluginToSubprojects()
        // could be default configuration
        // where `kotlin` plugin is applied
        applyPluginToKotlinProjects()
    }
}
```

Cons:

* on current moment there is no such Gradle DSL which is compatible with Isolated
  Projects: https://github.com/gradle/gradle/issues/22514

## 1.5 via settings plugin

In root `settings.gradle.kts` (not in `build.gradle.kts`)

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
    // or another id - depends on what we want/can
    id("org.jetbrains.dokka.settings")
}

dokka {
    // or similar DSL
    applyPluginToSubprojects()
    // 2 options below could be the default configuration
    // where `kotlin` plugin is applied
    applyPluginToKotlinProjects()
    applyPluginToRootProject()
}
```

Pros:

* should work fine with any kind of projects (even single-module ones)
* should work fine with Isolated Projects
* AGP also provides settings plugin in recent versions

Cons:

# 2. Aggregation setup

Requirements:

* allow setting up aggregation build in a root project (the most popular workflow)
* allow setting up aggregation build in some project (useful when dokka is just a part of the setup for docs)
* should work with Gradle restrictions

## 2.1 explicit gradle constructs

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

## 2.2 custom dokka DSL

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    aggregation {
        // will include all subprojects except `exclude` ones 
        includeSubprojects(exlcude = ":some-internal-module")

        includeProject(":explicit:path:to:project")


    }
}
```

Cons:

* if subproject doesn't have `dokka` applied we need to explicitly filter it (not possible to do otherwise to be
  compatible with Isolated Projects)

## 2.3 dokka DSL in settings

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
    // or another id - depends on what we want/can
    id("org.jetbrains.dokka.settings")
}

dokka {
    // by default will create an aggregate in root project
    aggregate {
        // similar DSL to the one above
        includeProjects(exclude = ":some-internal-module")
    }
    // aggregate in some other place
    aggregate(":docs") {
        includeProjects(exclude = ":some-internal-module")
    }
    // or other name
    multiModule(":docs") {
        includeProjects(exclude = ":some-internal-module")
    }
}
```

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

## 4.1 explicitly in projects (no sharing)

## 4.1 via convention plugins

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

## 4.2 in root project via custom DSL

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

Cons:

* it could be not easy to understand what is shared and what not
* it could be hard to do it with Isolated Projects in mind

## 4.3 in root project via `subprojects/allprojects`

```kotlin
// root build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    // configuration for root projects
}

subprojects {
    dokka {
        // configuration for subprojects
        includeEmptyPackages.set(true)
    }
}
```

## 4.4 via settings plugin

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
    // or another id - depends on what we want/can
    id("org.jetbrains.dokka.settings")
}

dokka {
    // will be applied to all projects in build
    includeEmptyPackages.set(true)
    // or via subdsl, to not mix all things in one place
    eachModule {
        includeEmptyPackages.set(true)
    }

    // matching by `path` 
    // (it may be possible to enhance and provide more information, depends on Gradle)
    perModule("*internal*") {
        suppress.set(true)
    }
}
```

# 5. Aggregation configuration vs Generation configuration

## Question 1

Should we have single DSL or 2 different DSLs for aggregation and generation parts

## Question 2

Aggregation project ambiguity: `root` (or other aggregation project) can have both sources and aggregation, TBD what
to do here, but looks like it

# 5. Adding Dokka Engine Plugins

same as in single-module projects. Though, some plugin should be applied only to root project (f.e. versioning)

Example via simple setup:

```kotlin
// root build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    // this should be applied in root only
    plugin(libs.dokka.versioning) {
        // configuration here
    }
}

subprojects {
    plugins.apply("org.jetbrains.dokka")
    dokka {
        // plugin for subprojects
        plugins(libs.dokka.kotlinAsJava)
    }
}
```

Example via settings.gradle.kts:

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    // version catalogs are not available in settings kts
    plugin("org.jetbrains.dokka:dokka-kotlinAsJava")
    // or
    eachModule {
        plugin("org.jetbrains.dokka:dokka-kotlinAsJava")
    }

    multiModule {
        plugin("org.jetbrains.dokka:dokka-versioning") {
            // configuration
        }
    }
}
```

# 6. HTML format specifics

same configuration for aggregate and not:

```kotlin
// root build.gradle.kts
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    html {
        customStyleSheets.from(rootDir.resolve("folder"))
    }
}

subprojects {
    plugins.apply("org.jetbrains.dokka")
    dokka {
        inheritFrom(rootProject)

        html {
            customStyleSheets.from(rootDir.resolve("folder"))
        }
    }
}
```

# 7. other(custom) formats

aggregation in other projects - for now we have no javadoc support for multi-module