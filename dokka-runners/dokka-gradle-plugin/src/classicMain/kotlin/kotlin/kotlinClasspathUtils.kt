/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.dokka.gradle.isAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.classpathOf(sourceSet: KotlinSourceSet): FileCollection {
    val compilations = compilationsOf(sourceSet)
    return if (compilations.isNotEmpty()) {
        compilations
            .map { compilation -> compilation.compileClasspathOf(project = this) }
            .reduce(FileCollection::plus)
    } else {
        // Dokka suppresses source sets that do no have compilations
        // since such configuration is invalid, it reports a warning or an error
        sourceSet.withAllDependentSourceSets()
            .map { it.kotlin.sourceDirectories }
            .reduce(FileCollection::plus)
    }
}

private fun KotlinCompilation.compileClasspathOf(project: Project): FileCollection {
    if (this.target.isAndroidTarget()) { // Workaround for https://youtrack.jetbrains.com/issue/KT-33893
        return (compileTaskProvider.get() as? KotlinCompile)?.libraries ?: project.files()
    }

    val result = project.objects.fileCollection()
    result.from({ compileDependencyFiles })

    val kgpVersion = project.getKgpVersion()
    // Since Kotlin 2.0 native distributiuon dependencies will be included to compileDependencyFiles
    if (kgpVersion != null && kgpVersion <= KotlinGradlePluginVersion(1, 9, 255)) {
        if (this is AbstractKotlinNativeCompilation) {
            val excludePlatformFiles = project.classpathProperty("excludeKonanPlatformDependencyFiles", default = false)
            if (!excludePlatformFiles) {
                val kotlinNativeDistributionAccessor =
                    @Suppress("DEPRECATION") KotlinNativeDistributionAccessor(project)
                result.from(kotlinNativeDistributionAccessor.stdlibDir)
                result.from(kotlinNativeDistributionAccessor.platformDependencies(konanTarget))
            }
        }
    }

    return result
}

private fun Project.classpathProperty(name: String, default: Boolean): Boolean =
    (findProperty("org.jetbrains.dokka.classpath.$name") as? String)?.toBoolean() ?: default
