/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.java

import com.intellij.openapi.project.Project
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.ProjectProvider
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerDescriptorAnalysisPlugin
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle

internal class KotlinAnalysisProjectProvider : ProjectProvider {
    override fun getProject(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): Project {
        val kotlinAnalysis = context.plugin<CompilerDescriptorAnalysisPlugin>().querySingle { kotlinAnalysis }
        return kotlinAnalysis[sourceSet].project
    }
}
