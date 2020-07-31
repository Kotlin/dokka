@file:Suppress("UnstableApiUsage")

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

    this.sourceRoots.from(sourceSet.sourceRoots)
    this.classpath.from(sourceSet.classpath)
    this.platform by Platform.fromString(sourceSet.platform.name)
    this.dependentSourceSets.set(dependentSourceSetIds)
    this.displayName by sourceSet.name.substringBeforeLast(
        delimiter = "Main",
        missingDelimiterValue = sourceSet.platform.name
    )
}

