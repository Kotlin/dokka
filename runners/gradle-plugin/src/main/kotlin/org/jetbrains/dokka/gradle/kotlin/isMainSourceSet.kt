package org.jetbrains.dokka.gradle.kotlin

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation

internal fun Project.isMainSourceSet(sourceSet: KotlinSourceSet): Boolean {
    return isMainSourceSet(allCompilationsOf(sourceSet))
}

internal fun isMainSourceSet(compilations: List<KotlinCompilation>): Boolean {
    if (compilations.isEmpty()) return true
    return compilations.any { compilation -> isMainCompilation(compilation) }
}

private fun isMainCompilation(compilation: KotlinCompilation): Boolean {
    try {
        val androidVariant = compilation.run { this as? KotlinJvmAndroidCompilation }?.androidVariant
        if (androidVariant != null) {
            return androidVariant is LibraryVariant || androidVariant is ApplicationVariant
        }
    } catch (e: NoSuchMethodError) {
        // Kotlin Plugin version below 1.4
        return !compilation.name.toLowerCase().endsWith("test")
    }
    return compilation.name == "main"
}
