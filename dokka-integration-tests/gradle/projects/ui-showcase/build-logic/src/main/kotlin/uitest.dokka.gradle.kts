/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.gradle.AbstractDokkaTask

plugins {
    id("org.jetbrains.dokka")
}

tasks.withType<AbstractDokkaTask>().configureEach {
    @Language("JSON")
    val versioningPluginConfiguration = """
    {
      "olderVersionsDir": "${project.rootProject.projectDir.resolve("previousDocVersions").invariantSeparatorsPath}"
    }
    """.trimIndent()

    pluginsMapConfiguration.set(
        mapOf(
            // fully qualified plugin name to json configuration
            "org.jetbrains.dokka.versioning.VersioningPlugin" to versioningPluginConfiguration
        )
    )
}

val dokka_it_dokka_version: String by project
dependencies {
    dokkaPlugin("org.jetbrains.dokka:versioning-plugin:$dokka_it_dokka_version")
}
