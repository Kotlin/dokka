package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget


internal fun Project.isAndroidProject() = try {
    project.extensions.getByName("android")
    true
} catch(e: UnknownDomainObjectException) {
    false
} catch(e: ClassNotFoundException) {
    false
}

internal fun Project.isNotMultiplatformProject() = !isMultiplatformProject()

internal fun Project.isMultiplatformProject() = try {
    project.extensions.getByType(KotlinMultiplatformExtension::class.java)
    true
} catch(e: UnknownDomainObjectException) {
    false
} catch (e: NoClassDefFoundError){
    false
} catch(e: ClassNotFoundException) {
    false
}

internal fun KotlinTarget.isAndroidTarget() = this.platformType == KotlinPlatformType.androidJvm