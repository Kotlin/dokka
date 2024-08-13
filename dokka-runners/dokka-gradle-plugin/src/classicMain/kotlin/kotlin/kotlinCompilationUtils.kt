/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.jetbrains.dokka.gradle.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation

internal typealias KotlinCompilation = org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<*>

internal fun Project.compilationsOf(sourceSet: KotlinSourceSet): List<KotlinCompilation> {
    //KT-45412 Make sure .kotlinSourceSets and .allKotlinSourceSets include the default source set
    val compilations = allCompilationsOf(sourceSet).filter { compilation ->
        sourceSet in compilation.kotlinSourceSets || sourceSet == compilation.defaultSourceSet
    }

    val hasAdditionalCommonCompatibilityMetadataVariant = compilations.size >= 2
            && this.isHmppEnabled()
            && compilations.any { it is KotlinCommonCompilation && it.compilationName == "main" }
            && compilations.any { it is KotlinCommonCompilation && it.compilationName == "commonMain" }

    return if (hasAdditionalCommonCompatibilityMetadataVariant) {
        // If the project has `kotlin.mpp.enableCompatibilityMetadataVariant` set to `true`
        // and it produces a legacy variant for common, we filter it out because one of the dependencies
        // might be published without it, and it would lead to the following error:
        //
        // > Execution failed for task ':project:dokkaHtmlPartial'.
        // > Could not resolve all files for configuration ':project:metadataCompileClasspath'.
        //    > Could not resolve com.example.dependency:0.1.0.
        //       > The consumer was configured to find a usage of 'kotlin-api' of a library, preferably optimized for
        //         non-jvm, as well as attribute 'org.jetbrains.kotlin.platform.type' with value 'common'. However we
        //         cannot choose between the following variants of com.example.dependency:0.1.0:
        //
        // This can be reproduced consistently on Ktor of version 2.3.2
        compilations.filterNot { it is KotlinCommonCompilation && it.compilationName == "main" }
    } else {
        compilations
    }
}

private fun Project.isHmppEnabled(): Boolean {
    // [KotlinCommonCompilation.isKlibCompilation] is internal, so we use this property instead.
    // The property name might seem misleading, but it's set by KGP if HMPP is enabled:
    // https://github.com/JetBrains/kotlin/blob/1.9.0/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/mpp/internal/hierarchicalStructureMigrationHandling.kt#L33
    return (this.findProperty("kotlin.mpp.enableGranularSourceSetsMetadata") as? String)?.toBoolean()
        ?: false
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
