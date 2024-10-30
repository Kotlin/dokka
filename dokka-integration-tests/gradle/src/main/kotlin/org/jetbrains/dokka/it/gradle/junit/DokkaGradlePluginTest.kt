/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

/**
 * A Dokka Gradle Plugin test.
 *
 * Annotate a test function with `@DokkaGradlePluginTest` to run the test multiple times with all
 * configured Android/Gradle/Kotlin/Dokka versions.
 *
 * Add a [DokkaGradleProjectRunner] parameter.
 *
 * [DokkaGradlePluginTestExtension] will generate a new project in a temporary directory for each test.
 */
@Target(FUNCTION)
@Retention(RUNTIME)
@TestTemplate
@ExtendWith(DokkaGradlePluginTestExtension::class)
annotation class DokkaGradlePluginTest(
    /** The name of source project directory, within [DokkaGradlePluginTestExtension.templateProjectsDir]. */
    val sourceProjectName: String,
    val projectInitializer: KClass<out GradleTestProjectInitializer> = GradleTestProjectInitializer.Default::class,
    val gradlePropertiesProvider: KClass<out GradlePropertiesProvider> = GradlePropertiesProvider.Default::class,
)
