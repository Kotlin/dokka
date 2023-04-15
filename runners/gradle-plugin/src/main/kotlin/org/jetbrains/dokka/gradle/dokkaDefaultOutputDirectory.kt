package org.jetbrains.dokka.gradle

import org.gradle.api.Task
import java.io.File
import java.util.Locale

internal fun Task.defaultDokkaOutputDirectory(): File {
    return defaultDokkaOutputDirectory(project.buildDir, name)
}

internal fun defaultDokkaOutputDirectory(buildDir: File, taskName: String): File {
    val formatClassifier = taskName.removePrefix("dokka").replaceFirstChar { it.lowercase(Locale.getDefault()) }
    return File(buildDir, "dokka${File.separator}$formatClassifier")
}
