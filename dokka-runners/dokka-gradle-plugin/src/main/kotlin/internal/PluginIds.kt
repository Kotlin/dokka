/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

/**
 * Gradle Plugin IDs.
 */
internal object PluginIds {

    val kotlin: Set<String> = setOf(
        "org.jetbrains.kotlin.android",
        "org.jetbrains.kotlin.js",
        "org.jetbrains.kotlin.jvm",
        "org.jetbrains.kotlin.multiplatform",
    )

    val android: Set<String> = setOf(
        "com.android.base",
        "com.android.application",
        "com.android.library",
        "com.android.test",
        "com.android.dynamic-feature",
        "com.android.kotlin.multiplatform.library",
    )
}
