package org.jetbrains.dokka.analysis.java

import com.intellij.openapi.project.Project
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

// TODO [beresnev] rename
interface JavaAnalysisHelper {
    fun extractSourceRoots(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): List<File>

    fun extractProject(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): Project

    fun createPsiParser(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): DokkaPsiParser
}
