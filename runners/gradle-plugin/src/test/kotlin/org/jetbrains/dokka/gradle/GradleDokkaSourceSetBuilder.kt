@file:Suppress("TestFunctionName")

package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.jetbrains.dokka.DokkaSourceSetID

fun GradleDokkaSourceSetBuilder(name: String, project: Project, sourceSetScopeId: String = "${project.path}:test"):
        GradleDokkaSourceSetBuilder {
    return GradleDokkaSourceSetBuilder(name, project) { DokkaSourceSetID(sourceSetScopeId, it) }
}
