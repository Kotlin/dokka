/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginExperimentalApi

/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    // this configuration will be shared to included projects
    includedDocumentation.from("README.md")
    html {
        customStyleSheets.from(
            listOf("style.css", "prism.css", "logo-styles.css").map { rootProject.file("dokka/$it") }
        )
        customAssets.from(
            listOf("apollo.svg").map { rootProject.file("dokka/$it") }
        )
    }

    aggregation {
        // in case of apollo it's done in the convention plugin
        // but for this example, we do it here
        applyPluginToIncludedProjects = true
        includeAllProjects {
            // some excludes if needed
        }
    }

    @OptIn(DokkaGradlePluginExperimentalApi::class)
    versioning {
        // fetched externally
        val olderVersions = listOf<String>()
        val downloadKDocVersions = tasks.register("dowloadKDocVersions")
        val currentVersionValue = ""

        currentVersion = currentVersionValue
        versionsOrdering = (olderVersions + currentVersionValue).reversed()
        oldVersionsDirectory = downloadKDocVersions.map { it.outputs.files.singleFile }
    }

    currentProject {
        /**
         * The Apollo docs website expect the contents to be in a `kdoc` subfolder
         * See https://github.com/apollographql/website-router/blob/389d6748c592ac88411ceb15c93965d2b800d9b3/_redirects#L105
         */
        outputDirectory = layout.buildDirectory.dir("dokka/html/kdoc")
    }
}
