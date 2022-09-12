package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.dokka.gradle.isAndroidTarget
import org.jetbrains.dokka.utilities.cast
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val Project.isHMPPEnabled
    // [KotlinCommonCompilation.isKlibCompilation] is internal, so we use this
    get() = (this.findProperty("kotlin.mpp.enableGranularSourceSetsMetadata") as? String)?.toBoolean() ?: false

internal fun Project.classpathOf(sourceSet: KotlinSourceSet): FileCollection {
    val compilations = compilationsOf(sourceSet)
    return if (compilations.isNotEmpty()) {
        compilations
            /**
             * If the project has enabled Compatibility Metadata Variant (produces legacy variant),
             * we don't touch it due to some dependant library
             * might be published without Compatibility Metadata Variant.
             * Dokka needs only HMPP variant
             * Ignore [org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation]  for `commonMain`  sourceSet with name `main`
             */
            .filterNot { compilation -> isHMPPEnabled && compilation is KotlinCommonCompilation && compilation.name == "main" }
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
        @Suppress("DEPRECATION") // for compatibility
        return compilation.compileKotlinTask.cast<KotlinCompile>().classpath
    }

    val platformDependencyFiles: FileCollection = (compilation as? AbstractKotlinNativeCompilation)
        ?.target?.project?.configurations
        ?.findByName(compilation.defaultSourceSet.implementationMetadataConfigurationName)
        ?: files()

    return compilation.compileDependencyFiles + platformDependencyFiles +
            @Suppress("DEPRECATION") // for compatibility
            (compilation.compileKotlinTask.run { this as? KotlinCompile }?.classpath ?: files())
}
