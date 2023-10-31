/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
    val kgpVersion = project.getKgpVersion()

    // if KGP version < 1.9 or org.jetbrains.dokka.classpath.useOldResolution=true
    // we will use old (pre 1.9) resolution of classpath
    if (kgpVersion == null ||
        kgpVersion < KotlinGradlePluginVersion(1, 9, 0) ||
        project.classpathProperty("useOldResolution", default = false)
    ) {
        return oldCompileClasspathOf(project)
    }

    return newCompileClasspathOf(project)
}

private fun KotlinCompilation.newCompileClasspathOf(project: Project): FileCollection {
    if (this.target.isAndroidTarget()) { // Workaround for https://youtrack.jetbrains.com/issue/KT-33893
        return this.classpathOf(project)
    }

    val result = project.objects.fileCollection()
    result.from({ compileDependencyFiles })

    val kgpVersion = project.getKgpVersion()
    // Since Kotlin 2.0 native distributiuon dependencies will be included to compileDependencyFiles
    if (kgpVersion != null && kgpVersion <= KotlinGradlePluginVersion(1, 9, 255)) {
        if (this is AbstractKotlinNativeCompilation) {
            val kotlinNativeDistributionAccessor = KotlinNativeDistributionAccessor(project)
            result.from(kotlinNativeDistributionAccessor.stdlibDir)
            result.from(kotlinNativeDistributionAccessor.platformDependencies(konanTarget))
        }
    }

    return result
}

private fun KotlinCompilation.oldCompileClasspathOf(project: Project): FileCollection {
    if (this.target.isAndroidTarget()) { // Workaround for https://youtrack.jetbrains.com/issue/KT-33893
        return this.classpathOf(project)
    }

    return this.compileDependencyFiles + platformDependencyFiles(project) + this.classpathOf(project)
}

private fun KotlinCompilation.classpathOf(project: Project): FileCollection {
    val kgpVersion = project.getKgpVersion()
    val kotlinCompile = this.getKotlinCompileTask(kgpVersion) ?: return project.files()

    val shouldKeepBackwardsCompatibility = (kgpVersion != null && kgpVersion < KotlinGradlePluginVersion(1, 7, 0))
    return if (shouldKeepBackwardsCompatibility) {
        // removed since 1.9.0, left for compatibility with < Kotlin 1.7
        val classpathGetter = kotlinCompile::class.members
            .first { it.name == "getClasspath" }
        classpathGetter.call(kotlinCompile) as FileCollection
    } else {
        kotlinCompile.libraries // introduced in 1.7.0
    }
}

private fun KotlinCompilation.getKotlinCompileTask(kgpVersion: KotlinGradlePluginVersion? = null): KotlinCompile? {
    val shouldKeepBackwardsCompatibility = (kgpVersion != null && kgpVersion < KotlinGradlePluginVersion(1, 8, 0))
    return if (shouldKeepBackwardsCompatibility) {
        @Suppress("DEPRECATION") // for `compileKotlinTask` property, deprecated with warning since 1.8.0
        this.compileKotlinTask as? KotlinCompile
    } else {
        this.compileTaskProvider.get() as? KotlinCompile // introduced in 1.8.0
    }
}

private fun KotlinCompilation.platformDependencyFiles(project: Project): FileCollection {
    val excludePlatformDependencyFiles = project.classpathProperty("excludePlatformDependencyFiles", default = false)

    if (excludePlatformDependencyFiles) return project.files()
    return (this as? AbstractKotlinNativeCompilation)
        ?.target?.project?.configurations
        ?.findByName(@Suppress("DEPRECATION") this.defaultSourceSet.implementationMetadataConfigurationName) // KT-58640
        ?: project.files()
}

private fun Project.classpathProperty(name: String, default: Boolean): Boolean =
    (findProperty("org.jetbrains.dokka.classpath.$name") as? String)?.toBoolean() ?: default
