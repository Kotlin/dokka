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

fun Project.configureDokkaCommon(): DokkatooExtension {
    apply {
        plugin("dev.adamko.dokkatoo-html")
    }
    val dokkatoo = extensions.getByType(DokkatooExtension::class.java)

    dokkatoo.apply {
        pluginsConfiguration.getByName("html") {
            this as DokkaHtmlPluginParameters
            customStyleSheets.from(
                listOf("style.css", "prism.css", "logo-styles.css").map { rootProject.file("dokka/$it") }
            )
            customAssets.from(
                listOf("apollo.svg").map { rootProject.file("dokka/$it") }
            )
        }
    }

    dokkatoo.dokkatooSourceSets.configureEach {
        includes.from("README.md")
    }

    return dokkatoo
}

fun Project.configureDokka() {
    configureDokkaCommon()
    val project = this
    val kdocProject = project(":apollo-kdoc")
    kdocProject.configurations.all {
        if (name == "dokkatoo") {
            this.dependencies.add(kdocProject.dependencies.project(mapOf("path" to project.path)))
        }
    }
}

fun Project.configureDokkaAggregate() {
    // fetched externally
    val olderVersions = listOf<String>()
    val downloadKDocVersions = tasks.register("dowloadKDocVersions")


    val dokkatoo = configureDokkaCommon()
    dependencies.add(
        "dokkatooPluginHtml",
        dokkatoo.versions.jetbrainsDokka.map { dokkaVersion ->
            "org.jetbrains.dokka:all-modules-page-plugin:$dokkaVersion"
        }
    )
    dependencies.add(
        "dokkatooPluginHtml",
        dokkatoo.versions.jetbrainsDokka.map { dokkaVersion ->
            "org.jetbrains.dokka:versioning-plugin:$dokkaVersion"
        }
    )

    dokkatoo.pluginsConfiguration.getByName("versioning") {
        this as DokkaVersioningPluginParameters
        val currentVersion = findProperty("VERSION_NAME") as String
        version.set(currentVersion)
        // Workaround for https://github.com/adamko-dev/dokkatoo/pull/135
        versionsOrdering.set((olderVersions + currentVersion).reversed())
        olderVersionsDir.fileProvider(downloadKDocVersions.map { it.outputs.files.singleFile })
    }
    tasks.withType(DokkatooGenerateTask::class.java).configureEach {
        /**
         * The Apollo docs website expect the contents to be in a `kdoc` subfolder
         * See https://github.com/apollographql/website-router/blob/389d6748c592ac88411ceb15c93965d2b800d9b3/_redirects#L105
         */
        outputDirectory.set(layout.buildDirectory.asFile.get().resolve("dokka/html/kdoc"))
    }
}
