package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.jetbrains.dokka.gradle.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal typealias KotlinCompilation =
        org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<KotlinCommonOptions>

internal fun Project.compilationsOf(sourceSet: KotlinSourceSet): List<KotlinCompilation> {
    //KT-45412 Make sure .kotlinSourceSets and .allKotlinSourceSets include the default source set
    return allCompilationsOf(sourceSet).filter { compilation ->
        sourceSet in compilation.kotlinSourceSets || sourceSet == compilation.defaultSourceSet
    }
}

internal fun Project.allCompilationsOf(
    sourceSet: KotlinSourceSet
): List<KotlinCompilation> {
    return when (val kotlin = kotlin) {
        is KotlinMultiplatformExtension -> allCompilationsOf(kotlin, sourceSet)
        is KotlinSingleTargetExtension<*> -> allCompilationsOf(kotlin, sourceSet)
        else -> emptyList()
    }
}

private fun allCompilationsOf(
    kotlin: KotlinMultiplatformExtension,
    sourceSet: KotlinSourceSet
): List<KotlinCompilation> {
    val allCompilations = kotlin.targets.flatMap { target -> target.compilations }
    return allCompilations.filter { compilation ->
        sourceSet in compilation.allKotlinSourceSets || sourceSet == compilation.defaultSourceSet
    }
}

private fun allCompilationsOf(
    kotlin: KotlinSingleTargetExtension<*>,
    sourceSet: KotlinSourceSet
): List<KotlinCompilation> {
    return kotlin.target.compilations.filter { compilation -> sourceSet in compilation.allKotlinSourceSets }
}
