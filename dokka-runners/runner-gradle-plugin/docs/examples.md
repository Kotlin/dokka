# Dokka Gradle Plugin DSL 2.0 - examples

## https://github.com/mamoe/mirai/blob/78446ab3670abb4b95b516032416c7f236b08854/build.gradle.kts#L87

Current Dokka:

```kotlin
tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaTask>().configureEach {
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        this.footerMessage = """Copyright 2019-${
            LocalDateTime.now().year
        } <a href="https://github.com/mamoe">Mamoe Technologies</a> and contributors.
            Source code:
            <a href="https://github.com/mamoe/mirai">GitHub</a>
            """.trimIndent()

        this.customAssets = listOf(
            rootProject.projectDir.resolve("mirai-dokka/frontend/ext.js"),
        )
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        perPackageOption {
            matchingRegex.set("net\\.mamoe\\.mirai\\.*")
            skipDeprecated.set(true)
        }

        for (suppressedPackage in arrayOf(
            """net.mamoe.mirai.internal""",
            """net.mamoe.mirai.internal.message""",
            """net.mamoe.mirai.internal.network""",
            """net.mamoe.mirai.console.internal""",
            """net.mamoe.mirai.console.compiler.common"""
        )) {
            perPackageOption {
                matchingRegex.set(suppressedPackage.replace(".", "\\."))
                suppress.set(true)
            }
        }
    }
}
```

New Dokka:

```kotlin
dokka {
    html {
        footerMessage.set(
            """Copyright 2019-${LocalDateTime.now().year} <a href="https://github.com/mamoe">Mamoe Technologies</a> and contributors.
            Source code:
            <a href="https://github.com/mamoe/mirai">GitHub</a>
            """.trimIndent()
        )
        customAssets.from(rootProject.projectDir.resolve("mirai-dokka/frontend/ext.js"))
    }
    // not sure why it's not top level
    perPackage("net.mamoe.mirai.*") {
        skipDeprecated.set(true)
    }
    for (suppressedPackage in arrayOf(
        "net.mamoe.mirai.internal",
        "net.mamoe.mirai.internal.message",
        "net.mamoe.mirai.internal.network",
        "net.mamoe.mirai.console.internal",
        "net.mamoe.mirai.console.compiler.common",
    )) {
        perPackage(suppressedPackage) {
            suppress.set(true)
        }
    }
}
```

## xodus

Current Dokka:

```kotlin
tasks.named<DokkaTask>("dokkaJavadoc") {
    dokkaSourceSets {
        configureEach {
            reportUndocumented.set(false)
        }
    }
}
```

New Dokka:

```kotlin
dokka {
    warnOnUndocumented.set(false)
}
```

## https://github.com/element-hq/element-android/blob/d418525748f6b4c9c42049fedf7d3ebf8963f20c/matrix-sdk-android/build.gradle#L23

Current Dokka:

```kotlin
dokkaHtml {
    dokkaSourceSets {
        configureEach {
            // Emit warnings about not documented members.
            // reportUndocumented.set(true)
            // Suppress legacy Riot's packages.
            perPackageOption {
                matchingRegex.set("org.matrix.android.sdk.internal.legacy.riot")
                suppress.set(true)
            }
            perPackageOption {
                matchingRegex.set("org.matrix.androidsdk.crypto.data")
                suppress.set(true)
            }
            // List of files with module and package documentation
            // https://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation
            includes.from("./docs/modules.md", "./docs/packages.md")
        }
    }
}
```

New Dokka:

```kotlin
dokka {
    perPackage("org.matrix.android.sdk.internal.legacy.riot") {
        suppress.set(true)
    }
    perPackage("org.matrix.androidsdk.crypto.data") {
        suppress.set(true)
    }
    includedDocumentation.from("./docs/modules.md", "./docs/packages.md")
}
```

## https://github.com/square/okhttp/blob/373822e239efa5db1e6ef44e779222966cea3b00/build.gradle.kts#L239

Current Dokka:

```kotlin
tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
        reportUndocumented.set(false)
        skipDeprecated.set(true)
        jdkVersion.set(8)
        perPackageOption {
            matchingRegex.set(".*\\.internal.*")
            suppress.set(true)
        }
        if (project.file("Module.md").exists()) {
            includes.from(project.file("Module.md"))
        }
        externalDocumentationLink {
            url.set(URI.create("https://square.github.io/okio/3.x/okio/").toURL())
            packageListUrl.set(URI.create("https://square.github.io/okio/3.x/okio/okio/package-list").toURL())
        }
    }
}
```

New Dokka:

```kotlin
dokka {
    warnOnUndocumented.set(false)
    includeDeprecated.set(false)
    perPackage("*.internal.*") {
        suppress.set(true)
    }
    if (project.file("Module.md").exists()) {
        includedDocumentation.from(project.file("Module.md"))
    }
    externalDocumentationLink(
        "https://square.github.io/okio/3.x/okio/",
        "https://square.github.io/okio/3.x/okio/okio/package-list"
    )
    sourceSets.configureEach {
        jdkVersion.set(8) // TODO: this parameter could be misleading
    }
}
```

## https://github.com/realm/realm-kotlin/blob/c08a3d370df97d0833f27d463dd759d651f5164b/packages/library-base/build.gradle.kts#L21

Current Dokka:

```kotlin
tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
    moduleName.set("Realm Kotlin SDK")
    moduleVersion.set(Realm.version)
    dokkaSourceSets {
        configureEach {
            moduleVersion.set(Realm.version)
            reportUndocumented.set(true)
            skipEmptyPackages.set(true)
            perPackageOption {
                matchingRegex.set(""".*\.internal.*""")
                suppress.set(true)
            }
            jdkVersion.set(8)
        }
        val commonMain by getting {
            includes.from(
                "overview.md",
                // TODO We could actually include package descriptions in top level overview file
                //  with:
                //    # package io.realm.kotlin
                //  Maybe worth a consideration
                "src/commonMain/kotlin/io/realm/kotlin/info.md",
                "src/commonMain/kotlin/io/realm/kotlin/log/info.md"
            )
            sourceRoot("../runtime-api/src/commonMain/kotlin")
        }
    }
}
```

New Dokka:

```kotlin
dokka {
    moduleName.set("Realm Kotlin SDK")
    moduleVersion.set(Realm.version)
    warnOnUndocumented.set(true)
    includeEmptyPackages.set(false)
    perPackage("*.internal*") {
        suppress.set(true)
    }
    sourceSets.named("commonMain") { // or `val commonMain by sourceSets.getting {`
        includedDocumentation.from(
            "overview.md",
            // TODO We could actually include package descriptions in top level overview file
            //  with:
            //    # package io.realm.kotlin
            //  Maybe worth a consideration
            "src/commonMain/kotlin/io/realm/kotlin/info.md",
            "src/commonMain/kotlin/io/realm/kotlin/log/info.md"
        )
    }
    sourceSets.configureEach {
        jdkVersion.set(8) // TODO: this parameter could be misleading
    }
}
```
