package org.jetbrains.conventions

import gradle.kotlin.dsl.accessors._0a1b0feab67dedd28ab92e8d9de7aa93.java
import org.jetbrains.DokkaBuildProperties

/**
 * A convention plugin that sets up common config and sensible defaults for all subprojects.
 *
 * It provides the [DokkaBuildProperties] extension, for accessing common build properties.
 */

plugins {
    base
}

java {
    toolchain {
        languageVersion.set(dokkaBuild.mainJavaVersion)
    }
}

val dokkaBuildProperties: DokkaBuildProperties = extensions.create(DokkaBuildProperties.EXTENSION_NAME)

if (project != rootProject) {
    project.group = rootProject.group
    project.version = rootProject.version
}
