/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.utils

import org.gradle.api.Project
import java.util.*

fun Project.enableV1Plugin(
    noWarn: Boolean = true
): Project {
    writeGradleProperties(project) {
        dokka.pluginMode = "V1Enabled"
        dokka.pluginModeNoWarn = noWarn
    }

    return this
}

fun Project.enableV2Plugin(
    v2MigrationHelpers: Boolean = true,
    noWarn: Boolean = true
): Project {
    writeGradleProperties(project) {
        dokka.pluginMode = if (v2MigrationHelpers) "V2EnabledWithHelpers" else "V2Enabled"
        dokka.pluginModeNoWarn = noWarn
    }
    return this
}

private fun writeGradleProperties(
    project: Project,
    config: GradlePropertiesBuilder.() -> Unit,
) {
    val file = project.projectDir.resolve("gradle.properties")
    if (!file.exists()) {
        file.parentFile.mkdirs()
        file.createNewFile()
    }

    val properties = GradlePropertiesBuilder()
        .apply(config)
        .build()

    val existingProperties = Properties().apply {
        file.inputStream().use { load(it) }
    }

    existingProperties.putAll(properties)

    file.outputStream().use {
        existingProperties.store(it, null)
    }
}
