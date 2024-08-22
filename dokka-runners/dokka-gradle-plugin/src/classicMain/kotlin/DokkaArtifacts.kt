/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.jetbrains.dokka.DokkaVersion

internal val Project.dokkaArtifacts get() = DokkaArtifacts(this)

internal class DokkaArtifacts(private val project: Project) {
    private fun fromModuleName(name: String): Dependency =
        project.dependencies.create("org.jetbrains.dokka:$name:${DokkaVersion.version}")

    /** K1 Analysis */
    val analysisKotlinDescriptors get() = fromModuleName("analysis-kotlin-descriptors")

    /** K2 Analysis */
    val analysisKotlinSymbols get() = fromModuleName("analysis-kotlin-symbols")

    val allModulesPage get() = fromModuleName("all-modules-page-plugin")
    val dokkaCore get() = fromModuleName("dokka-core")
    val dokkaBase get() = fromModuleName("dokka-base")
    val javadocPlugin get() = fromModuleName("javadoc-plugin")
    val gfmPlugin get() = fromModuleName("gfm-plugin")
    val gfmTemplateProcessing get() = fromModuleName("gfm-template-processing-plugin")
    val jekyllTemplateProcessing get() = fromModuleName("jekyll-template-processing-plugin")
    val jekyllPlugin get() = fromModuleName("jekyll-plugin")
}
