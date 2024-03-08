/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// convention plugin

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    configureDokka()
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    configureDokka()
}

// aggregate not in root
if (project.path == ":apollo-kdoc") configureDokkaAggregate()

fun Project.configureDokkaCommon(): DokkaExtension {
    apply {
        plugin("org.jetbrains.dokka")
    }
    val dokka = extensions.getByType(DokkaExtension::class.java)

    dokka.apply {
        html {
            customStyleSheets.from(
                listOf("style.css", "prism.css", "logo-styles.css").map { rootProject.file("dokka/$it") }
            )
            customAssets.from(
                listOf("apollo.svg").map { rootProject.file("dokka/$it") }
            )
        }
        includedDocumentation.from("README.md")
    }

    return dokka
}

fun Project.configureDokka() {
    configureDokkaCommon()
}

fun Project.configureDokkaAggregate() {
    // fetched externally
    val olderVersions = listOf<String>()
    val downloadKDocVersions = tasks.register("dowloadKDocVersions")


    val dokka = configureDokkaCommon()

    dokka.apply {
        aggregation {
            // because it's not a root project, aggregation is not enabled by default
            // enabling this, will cause to consume partials from ALL other projects
            includeAllProjects()
        }
    }

    dokka.apply {
        plugin(
            "org.jetbrains.dokka:versioning-plugin:$dokkaVersion",
            "org.jetbrains.dokka.versioning.VersioningPlugin"
        ) {
            val currentVersion = findProperty("VERSION_NAME") as String
            property("version", currentVersion)
            property("versionsOrdering", (olderVersions + currentVersion).reversed())
            property("olderVersionsDir", downloadKDocVersions.map { it.outputs.files.singleFile })
        }
    }

    dokka.apply {
        /**
         * The Apollo docs website expect the contents to be in a `kdoc` subfolder
         * See https://github.com/apollographql/website-router/blob/389d6748c592ac88411ceb15c93965d2b800d9b3/_redirects#L105
         */
        outputDirectory.set(layout.buildDirectory.asFile.get().resolve("dokka/html/kdoc"))
    }
}
