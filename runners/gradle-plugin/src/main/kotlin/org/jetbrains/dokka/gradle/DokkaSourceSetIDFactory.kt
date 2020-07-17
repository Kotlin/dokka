@file:Suppress("FunctionName")

package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.jetbrains.dokka.DokkaSourceSetID

internal fun DokkaSourceSetID(project: Project, sourceSetName: String): DokkaSourceSetID {
    return DokkaSourceSetID(moduleName = project.path, sourceSetName = sourceSetName)
}
