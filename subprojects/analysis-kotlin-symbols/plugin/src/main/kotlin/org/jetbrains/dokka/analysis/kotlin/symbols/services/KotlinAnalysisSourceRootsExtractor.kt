package org.jetbrains.dokka.analysis.kotlin.symbols.services

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.SourceRootsExtractor
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

class KotlinAnalysisSourceRootsExtractor : SourceRootsExtractor {
    override fun extract(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): List<File> {
        return sourceSet.sourceRoots.filter { directory -> directory.isDirectory || directory.extension == "java" }
    }
}
