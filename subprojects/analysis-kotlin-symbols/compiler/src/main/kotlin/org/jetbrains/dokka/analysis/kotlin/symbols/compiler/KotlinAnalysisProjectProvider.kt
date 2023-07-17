package org.jetbrains.dokka.analysis.kotlin.symbols.compiler

import com.intellij.openapi.project.Project
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.ProjectProvider
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle

internal class KotlinAnalysisProjectProvider : ProjectProvider {
    override fun getProject(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): Project {
        val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
        return kotlinAnalysis[sourceSet].project
    }
}
