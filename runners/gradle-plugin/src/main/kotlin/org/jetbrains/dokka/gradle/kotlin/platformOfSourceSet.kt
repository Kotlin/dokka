package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal fun Project.platformOf(sourceSet: KotlinSourceSet): KotlinPlatformType {
    val targetNames = allCompilationsOf(sourceSet).map { compilation -> compilation.target.platformType }.distinct()
    return when (targetNames.size) {
        1 -> targetNames.single()
        else -> KotlinPlatformType.common
    }
}
