/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation

internal fun Project.isMainSourceSet(sourceSet: KotlinSourceSet): Boolean {
    return isMainSourceSet(allCompilationsOf(sourceSet))
}

internal fun isMainSourceSet(compilations: List<KotlinCompilation>): Boolean {
    return compilations.any { compilation -> isMainCompilation(compilation) }
}

private fun isMainCompilation(compilation: KotlinCompilation): Boolean {
    try {
        val androidVariant = compilation.run { this as? KotlinJvmAndroidCompilation }?.androidVariant
        if (androidVariant != null) {
            @Suppress("DEPRECATION")
            return androidVariant is com.android.build.gradle.api.LibraryVariant
                    || androidVariant is com.android.build.gradle.api.ApplicationVariant
        }
    } catch (e: NoSuchMethodError) {
        // Kotlin Plugin version below 1.4
        return !compilation.name.toLowerCase().endsWith("test")
    }
    return compilation.name == "main"
}
