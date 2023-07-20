package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.dokka.gradle.isAndroidTarget
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
            .map { compilation -> compilation.compileClasspathOf(project = this) }
            .reduce { acc, fileCollection -> acc + fileCollection }
    } else {
        // Dokka suppresses source sets that do no have compilations
        // since such configuration is invalid, it reports a warning or an error
        sourceSet.withAllDependentSourceSets()
            .toList()
            .map { it.kotlin.sourceDirectories }
            .reduce { acc, fileCollection -> acc + fileCollection }
    }
}

private fun KotlinCompilation.compileClasspathOf(project: Project): FileCollection {
    if (this.target.isAndroidTarget()) { // Workaround for https://youtrack.jetbrains.com/issue/KT-33893
        return this.classpathOf(project)
    }

    val platformDependencyFiles: FileCollection = (this as? AbstractKotlinNativeCompilation)
        ?.target?.project?.configurations
        ?.findByName(@Suppress("DEPRECATION") this.defaultSourceSet.implementationMetadataConfigurationName) // KT-58640
        ?: project.files()

    return this.compileDependencyFiles + platformDependencyFiles + this.classpathOf(project)
}

private fun KotlinCompilation.classpathOf(project: Project): FileCollection {
    val kgpVersion = project.getKgpVersion()
    val kotlinCompile = this.getKotlinCompileTask(kgpVersion) ?: return project.files()

    val shouldKeepBackwardsCompatibility = (kgpVersion != null && kgpVersion < KotlinGradlePluginVersion(1, 7, 0))
    return if (shouldKeepBackwardsCompatibility) {
        // removed since 1.9.0, left for compatibility with < Kotlin 1.7
        val classpathGetter= kotlinCompile::class.members
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
