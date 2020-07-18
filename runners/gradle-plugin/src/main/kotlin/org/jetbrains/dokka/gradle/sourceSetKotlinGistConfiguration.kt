package org.jetbrains.dokka.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

// TODO NOW: Test
fun GradleDokkaSourceSetBuilder.configureWithKotlinSourceSet(sourceSet: KotlinSourceSet) {
    configureWithKotlinSourceSetGist(project.kotlinExtension.gistOf(sourceSet))
}

internal fun GradleDokkaSourceSetBuilder.configureWithKotlinSourceSetGist(sourceSet: KotlinSourceSetGist) {
    sourceRoots.addAll(sourceRoots.union(sourceSet.sourceRoots.toSourceRoots()).distinct())
    dependentSourceSets.addAll(dependentSourceSets)
    dependentSourceSets.addAll(sourceSet.dependentSourceSets.map { DokkaSourceSetID(project, it) })
    classpath = classpath.union(sourceSet.classpath).distinct()
    if (platform == null && sourceSet.platform != "")
        platform = sourceSet.platform
}

private fun Iterable<File>.toSourceRoots(): List<GradleSourceRootBuilder> =
    this.filter { it.exists() }.map { GradleSourceRootBuilder().apply { directory = it } }
