# Auxiliary documentation

1. Add the plugin to classpath

```kotlin
// build.gradle.kts
buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:auxiliary-docs:$dokka_version")
    }
}
```

2. Add the plugin to dependencies

```kotlin
dependencies {
    dokkaHtmlPlugin("org.jetbrains.dokka:auxiliary-docs:$dokka_version")
}
```

3. Configure md files location

```kotlin

tasks.dokkaHtml.configure {
    pluginConfiguration<org.jetbrains.dokka.auxiliarydocs.AuxiliaryDocsPlugin, org.jetbrains.dokka.auxiliarydocs.AuxiliaryConfiguration> {
        docs = setOf(File("readme.md"))
    }
}
```
