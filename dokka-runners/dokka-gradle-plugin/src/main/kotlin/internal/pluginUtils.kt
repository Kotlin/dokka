/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.internal

import org.gradle.api.Project

/**
 * Try and get an extension from [Project.getExtensions], or `null` if it's not present.
 *
 * Handles as many errors as possible.
 *
 * Logs a warning if the extension is present, but the wrong type
 * (probably caused by an inconsistent buildscript classpath https://github.com/gradle/gradle/issues/27218)
 */
internal inline fun <reified T : Any> Project.findExtensionLenient(
    extensionName: String,
): T? {
    val candidate = extensions.findByName(extensionName)
        ?: return null

    val extensionInstance =
        try {
            candidate as? T
        } catch (e: Throwable) {
            when (e) {
                is TypeNotPresentException,
                is ClassNotFoundException,
                is NoClassDefFoundError -> {
                    logger.info("Dokka Gradle plugin failed to find extension ${T::class.simpleName}. ${e::class} ${e.message}")
                    null
                }

                else -> throw e
            }
        }

    if (extensionInstance == null) {
        if (project.extensions.findByName(extensionName) != null) {
            // uh oh - extension is present, but it's the wrong type
            // Is there a class loader issue? https://github.com/gradle/gradle/issues/27218
            logger.warn {
                val allPlugins =
                    project.plugins.joinToString { it::class.qualifiedName ?: "${it::class}" }
                val allExtensions =
                    project.extensions.extensionsSchema.elements.joinToString { "${it.name} ${it.publicType}" }

                """
                |Dokka Gradle plugin failed to get AndroidComponentsExtension in ${project.path}
                |  Applied plugins: $allPlugins
                |  Available extensions: $allExtensions
                """.trimMargin()
            }
        }
    }

    return extensionInstance
}
