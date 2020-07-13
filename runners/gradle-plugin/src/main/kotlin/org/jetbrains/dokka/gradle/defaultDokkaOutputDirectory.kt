package org.jetbrains.dokka.gradle

import org.gradle.api.Task
import java.io.File

internal fun Task.defaultDokkaOutputDirectory(): File {
    return defaultDokkaOutputDirectory(project.buildDir, name)
}

internal fun defaultDokkaOutputDirectory(buildDir: File, taskName: String): File {
    return File(buildDir, "dokka${File.separator}${formatClassifier(taskName)}")
}

internal fun formatClassifier(dokkaTaskName: String): String {
    return dokkaTaskName.removePrefix("dokka").decapitalize()
}
