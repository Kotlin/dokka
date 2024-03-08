/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

// aggregate build.gradle.kts

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    // fetched externally
    val olderVersions = listOf<String>()
    val downloadKDocVersions = tasks.register("dowloadKDocVersions")

    aggregation {
        // because it's not a root project, it's not enabled by default
        // enabling this, will cause to consume partials from ALL other projects
        enabled.set(true)
    }

    plugin(
        "org.jetbrains.dokka:versioning-plugin:$dokkaVersion",
        "org.jetbrains.dokka.versioning.VersioningPlugin"
    ) {
        val currentVersion = findProperty("VERSION_NAME") as String
        property("version", currentVersion)
        property("versionsOrdering", (olderVersions + currentVersion).reversed())
        property("olderVersionsDir", downloadKDocVersions.map { it.outputs.files.singleFile })
    }

    /**
     * The Apollo docs website expect the contents to be in a `kdoc` subfolder
     * See https://github.com/apollographql/website-router/blob/389d6748c592ac88411ceb15c93965d2b800d9b3/_redirects#L105
     */
    outputDirectory.set(layout.buildDirectory.asFile.get().resolve("dokka/html/kdoc"))
}
