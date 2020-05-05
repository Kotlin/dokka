package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.plugability.DokkaContext

data class SourceSetData(
    val moduleName: String,
    val sourceSetName: String,
    val platform: Platform,
    val sourceRoots: List<DokkaConfiguration.SourceRoot> = emptyList()
)

class SourceSetCache {
    private val sourceSets = HashMap<String, SourceSetData>()

    fun getSourceSet(pass: DokkaConfiguration.PassConfiguration) =
        sourceSets.getOrPut("${pass.moduleName}/${pass.sourceSetName}",
            { SourceSetData(pass.moduleName, pass.sourceSetName, pass.analysisPlatform, pass.sourceRoots) }
        )
}

fun DokkaContext.sourceSet(pass: DokkaConfiguration.PassConfiguration) : SourceSetData = sourceSetCache.getSourceSet(pass)