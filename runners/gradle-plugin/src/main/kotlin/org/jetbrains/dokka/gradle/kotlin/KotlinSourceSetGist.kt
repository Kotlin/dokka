package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

// TODO NOW: Test this all

internal data class KotlinSourceSetGist(
    val name: String,
    val platform: KotlinPlatformType,
    val isMain: Boolean,
    val classpath: FileCollection,
    val sourceRoots: FileCollection,
    val dependentSourceSetNames: Provider<Set<String>>,
)

internal fun Project.gistOf(sourceSet: KotlinSourceSet): KotlinSourceSetGist {
    return KotlinSourceSetGist(
        name = sourceSet.name,
        platform = platformOf(sourceSet),
        isMain = isMainSourceSet(sourceSet),
        classpath = classpathOf(sourceSet),
        // TODO: Needs to respect filters.
        //  We probably need to change from "sourceRoots" to support "sourceFiles"
        //  https://github.com/Kotlin/dokka/issues/1215
        sourceRoots = sourceSet.kotlin.sourceDirectories.filter { it.exists() },
        dependentSourceSetNames = project.provider { sourceSet.dependsOn.map { it.name }.toSet() },
    )
}
