package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException


fun Project.isAndroidProject() = try {
    project.extensions.getByName("android")
    true
} catch(e: UnknownDomainObjectException) {
    false
} catch(e: ClassNotFoundException) {
    false
}

fun DokkaTask.isMultiplatformProject() = this.multiplatform.isNotEmpty()