/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

/**
 * Gradle Plugin IDs.
 */
@Suppress("ConstPropertyName")
internal object PluginId {

    private const val KotlinAndroid = "org.jetbrains.kotlin.android"
    private const val KotlinJs = "org.jetbrains.kotlin.js"
    private const val KotlinJvm = "org.jetbrains.kotlin.jvm"
    private const val KotlinMultiplatform = "org.jetbrains.kotlin.multiplatform"
    val kgpPlugins: Set<String> = setOf(
        KotlinAndroid,
        KotlinJs,
        KotlinJvm,
        KotlinMultiplatform,
    )

    private const val AndroidBase = "com.android.base"
    private const val AndroidApplication = "com.android.application"
    private const val AndroidLibrary = "com.android.library"
    val androidPlugins: Set<String> = setOf(
        AndroidBase,
        AndroidApplication,
        AndroidLibrary,
    )
}
