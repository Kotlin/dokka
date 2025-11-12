/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.internal

import org.gradle.api.Project

/**
 * Try and get an extension from [Project.getExtensions], or `null` if it's not present.
 *
 * If [T] is not accessible in the current classloader, returns `null`.
 *
 * Logs a warning if the extension is present, but the wrong type
 * (probably caused by an inconsistent buildscript classpath https://github.com/gradle/gradle/issues/27218)
 */
internal inline fun <reified T : Any> Project.findExtensionLenient(
    extensionName: String,
): T? {

    val extensionByName = extensions.findByName(extensionName)
    if (extensionByName == null) {
        logger.info("Dokka Gradle plugin failed to find extension $extensionName by name ${T::class.java}")
        return null
    }

    try {
        return extensions.findByType(T::class.java)
    } catch (e: Throwable) {
        when (e) {
            is TypeNotPresentException,
            is ClassNotFoundException,
            is NoClassDefFoundError -> {

                // uh oh - extension is present, but it's the wrong type
                // Is there a class loader issue? https://github.com/gradle/gradle/issues/27218
                logger.warn {
                    // If we're here, then T isn't available, so don't use T::class.
                    // Instead, use the available extension's class.
                    val actualExtensionFqn =
                        extensions.extensionsSchema.firstOrNull { it.name == extensionName }?.publicType?.fullyQualifiedName

                    val allPlugins =
                        project.plugins.joinToString { it::class.qualifiedName ?: "${it::class.java}" }
                    val allExtensions =
                        project.extensions.extensionsSchema.elements.joinToString { "${it.name} ${it.publicType}" }

                    """
                    |Dokka Gradle plugin failed to get extension $extensionName $actualExtensionFqn in ${project.path}
                    |Please make sure plugins in all subprojects are consistent. See https://github.com/gradle/gradle/issues/27218
                    |  Applied plugins: $allPlugins
                    |  Available extensions: $allExtensions
                    """.trimMargin()
                }

                return null
            }

            else -> throw e
        }
    }
}
