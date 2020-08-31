package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.dokka.gradle.isAndroidTarget
import org.jetbrains.dokka.utilities.cast
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.classpathOf(sourceSet: KotlinSourceSet): FileCollection {
    val compilations = compilationsOf(sourceSet)
    return if (compilations.isNotEmpty()) {
        compilations
            .map { compilation -> compileClasspathOf(compilation) }
            .reduce { acc, fileCollection -> acc + fileCollection }
    } else {
        sourceSet.withAllDependentSourceSets()
            .toList()
            .map { it.kotlin.sourceDirectories }
            .reduce { acc, fileCollection -> acc + fileCollection }
    }
}

private fun Project.compileClasspathOf(compilation: KotlinCompilation): FileCollection {
    if (compilation.target.isAndroidTarget()) {
        // This is a workaround for https://youtrack.jetbrains.com/issue/KT-33893
        return compilation.compileKotlinTask.cast<KotlinCompile>().classpath
    }

    val platformDependencyFiles: FileCollection = (compilation as? KotlinNativeCompilation)
        ?.target?.project?.configurations
        ?.findByName(compilation.defaultSourceSet.implementationMetadataConfigurationName)
        ?: files()

    return compilation.compileDependencyFiles + platformDependencyFiles +
            (compilation.compileKotlinTask.run { this as? KotlinCompile }?.classpath ?: files())
}
