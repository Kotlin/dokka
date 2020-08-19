package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal data class KotlinSourceSetGist(
    val name: String,
    val platform: Provider<KotlinPlatformType>,
    val isMain: Provider<Boolean>,
    val classpath: Provider<FileCollection>,
    val sourceRoots: FileCollection,
    val dependentSourceSetNames: Provider<Set<String>>,
)

internal fun Project.gistOf(sourceSet: KotlinSourceSet): KotlinSourceSetGist = KotlinSourceSetGist(
    name = sourceSet.name,
    platform = project.provider { platformOf(sourceSet) },
    isMain = project.provider { isMainSourceSet(sourceSet) },
    classpath = project.provider { classpathOf(sourceSet).filter { it.exists() } },
    // TODO: Needs to respect filters.
    //  We probably need to change from "sourceRoots" to support "sourceFiles"
    //  https://github.com/Kotlin/dokka/issues/1215
    sourceRoots = sourceSet.kotlin.sourceDirectories.filter { it.exists() },
    dependentSourceSetNames = project.provider { sourceSet.dependsOn.map { it.name }.toSet() },
)

