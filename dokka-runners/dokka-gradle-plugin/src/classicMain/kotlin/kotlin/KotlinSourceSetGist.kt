/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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
    sourceRoots = sourceSet.kotlin.sourceDirectories.filter { it.exists() },
    dependentSourceSetNames = project.provider { sourceSet.dependsOn.map { it.name }.toSet() },
)
