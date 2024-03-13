/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.dokka.gradle.internal.DefaultDokkaProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

// just for the creation of a task
@DokkaGradlePluginExperimentalApi
public fun DokkaProjectExtension.registerDokkaJarTask(
    classifier: String = "javadoc",
    variant: String = "main",
    configure: Jar.() -> Unit = {}
): TaskProvider<Jar> = (this as DefaultDokkaProjectExtension).project.tasks
    .register<Jar>("dokka${classifier.uppercaseFirstChar()}Jar", configure)

// if we want auto-wiring to Kotlin publications
@DokkaGradlePluginExperimentalApi
public fun KotlinMultiplatformExtension.withDokkaJar(
    classifier: String = "javadoc",
    variant: String = "main",
    configure: Jar.() -> Unit = {}
) {
}

@DokkaGradlePluginExperimentalApi
public fun KotlinJvmProjectExtension.withDokkaJar(
    classifier: String = "javadoc",
    variant: String = "main",
    configure: Jar.() -> Unit = {}
) {
}

@DokkaGradlePluginExperimentalApi
public fun KotlinTarget.withDokkaJar(
    classifier: String = "javadoc",
    variant: String = "main",
    configure: Jar.() -> Unit = {}
) {
}
