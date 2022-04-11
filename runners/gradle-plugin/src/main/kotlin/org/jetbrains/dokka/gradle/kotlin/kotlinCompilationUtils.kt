package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.jetbrains.dokka.gradle.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal typealias KotlinCompilation =
        org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<KotlinCommonOptions>

internal fun Project.allCompilationsOf(
    sourceSet: KotlinSourceSet
): List<KotlinCompilation> {
    return when (val kotlin = kotlin) {
        is KotlinMultiplatformExtension -> kotlin.allCompilationsOf(sourceSet)
        is KotlinSingleTargetExtension -> kotlin.allCompilationsOf(sourceSet)
        else -> emptyList()
    }
}

internal fun Project.compilationsOf(sourceSet: KotlinSourceSet): List<KotlinCompilation> {
    //KT-45412 Make sure .kotlinSourceSets and .allKotlinSourceSets include the default source set
    return allCompilationsOf(sourceSet).filter { compilation -> sourceSet in compilation.kotlinSourceSets || sourceSet == compilation.defaultSourceSet }
}

private fun KotlinMultiplatformExtension.allCompilationsOf(sourceSet: KotlinSourceSet): List<KotlinCompilation> {
    val allCompilations = targets.flatMap { target -> target.compilations }
    return allCompilations.filter { compilation -> sourceSet in compilation.allKotlinSourceSets || sourceSet == compilation.defaultSourceSet }
}

private fun KotlinSingleTargetExtension.allCompilationsOf(sourceSet: KotlinSourceSet): List<KotlinCompilation> {
    return target.compilations.filter { compilation -> sourceSet in compilation.allKotlinSourceSets }
}
