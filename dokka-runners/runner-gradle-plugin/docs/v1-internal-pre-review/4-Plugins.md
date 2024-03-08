# Dokka Gradle Plugin DSL 2.0—working with plugins

# 3. Adding Dokka Engine Plugins—From single module projects

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

# 5. Adding Dokka Engine Plugins—From multi module projects

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
