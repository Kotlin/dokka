/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.jetbrains.dokka.DokkaSourceSetID

@Suppress("TestFunctionName")
fun GradleDokkaSourceSetBuilder(name: String, project: Project, sourceSetScopeId: String = "${project.path}:test"):
        GradleDokkaSourceSetBuilder {
    return GradleDokkaSourceSetBuilder(name, project) { DokkaSourceSetID(sourceSetScopeId, it) }
}
