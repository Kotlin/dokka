package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.gradle.kotlin.KotlinSourceSetGist
import org.jetbrains.dokka.gradle.kotlin.gistOf
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

// TODO NOW: Test
fun GradleDokkaSourceSetBuilder.configureWithKotlinSourceSet(sourceSet: KotlinSourceSet) {
    configureWithKotlinSourceSetGist(project.gistOf(sourceSet))
}

// TODO NOW: What about language version?
internal fun GradleDokkaSourceSetBuilder.configureWithKotlinSourceSetGist(sourceSet: KotlinSourceSetGist) {
    val dependentSourceSetIds = sourceSet.dependentSourceSetNames.map { sourceSetNames ->
        sourceSetNames.map { sourceSetName -> DokkaSourceSetID(sourceSetName) }
    }

    this.sourceRoots.from(sourceSet.sourceRoots)
    this.classpath.from(sourceSet.classpath)
    this.platform by sourceSet.platform.name
    this.dependentSourceSets.set(dependentSourceSetIds)
}


