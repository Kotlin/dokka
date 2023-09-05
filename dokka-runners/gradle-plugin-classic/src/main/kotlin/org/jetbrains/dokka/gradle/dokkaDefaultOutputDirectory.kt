/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.Task
import java.io.File

internal fun Task.defaultDokkaOutputDirectory(): File {
    return defaultDokkaOutputDirectory(project.buildDir, name)
}

internal fun defaultDokkaOutputDirectory(buildDir: File, taskName: String): File {
    val formatClassifier = taskName.removePrefix("dokka").decapitalize()
    return File(buildDir, "dokka${File.separator}$formatClassifier")
}
