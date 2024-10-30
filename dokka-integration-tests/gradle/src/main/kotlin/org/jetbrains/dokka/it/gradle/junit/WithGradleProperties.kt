/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.jetbrains.dokka.gradle.utils.GradlePropertiesBuilder

/**
 * Add this annotation to static functions that return [GradlePropertiesProvider].
 * The provided Gradle properties will be appended to all tests in the current test context.
 *
 * The function *must* be annotated with [`@JvmStatic`][JvmStatic] due to JUnit limitations.
 */
annotation class WithGradleProperties

/**
 * Provide Gradle properties to be added to the `gradle.properties` file in the root directory of the tested project.
 *
 * The provided properties will be appended to any existing properties.
 */
fun interface GradlePropertiesProvider {
    fun get(): Map<String, String>

    object Default : GradlePropertiesProvider {
        override fun get(): Map<String, String> {
            return GradlePropertiesBuilder().build()
        }
    }

    object DefaultAndroid : GradlePropertiesProvider {
        override fun get(): Map<String, String> {
            return GradlePropertiesBuilder().build()
        }
    }
}
