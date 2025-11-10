/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

/**
 * Gradle Plugin IDs.
 */
@Suppress("ConstPropertyName")
internal object PluginId {

    const val KotlinAndroid = "org.jetbrains.kotlin.android"
    const val KotlinJs = "org.jetbrains.kotlin.js"
    const val KotlinJvm = "org.jetbrains.kotlin.jvm"
    const val KotlinMultiplatform = "org.jetbrains.kotlin.multiplatform"
    val kgpPlugins: Set<String> = setOf(
        KotlinAndroid,
        KotlinJs,
        KotlinJvm,
        KotlinMultiplatform,
    )

    const val AndroidBase = "com.android.base"
    const val AndroidApplication = "com.android.application"
    const val AndroidLibrary = "com.android.library"
    const val AndroidTest = "com.android.test"
    const val AndroidDynamicFeature = "com.android.dynamic-feature"
}
