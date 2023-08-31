/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.kotlin.KotlinSourceSetGist
import org.jetbrains.dokka.gradle.kotlin.gistOf
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

fun GradleDokkaSourceSetBuilder.configureWithKotlinSourceSet(sourceSet: KotlinSourceSet) {
    configureWithKotlinSourceSetGist(project.gistOf(sourceSet))
}

internal fun GradleDokkaSourceSetBuilder.configureWithKotlinSourceSetGist(sourceSet: KotlinSourceSetGist) {
    val dependentSourceSetIds = sourceSet.dependentSourceSetNames.map { sourceSetNames ->
        sourceSetNames.map { sourceSetName -> DokkaSourceSetID(sourceSetName) }
    }

    this.suppress.convention(sourceSet.isMain.map { !it })
    this.sourceRoots.from(sourceSet.sourceRoots)
    this.classpath.from(sourceSet.classpath)
    this.platform.convention(sourceSet.platform.map { Platform.fromString(it.name) })
    this.dependentSourceSets.convention(dependentSourceSetIds)
    this.displayName.convention(sourceSet.platform.map { platform ->
        sourceSet.name.substringBeforeLast(
            delimiter = "Main",
            missingDelimiterValue = platform.name
        )
    })
}
