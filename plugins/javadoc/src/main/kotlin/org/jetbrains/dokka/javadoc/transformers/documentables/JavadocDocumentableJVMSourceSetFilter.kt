package org.jetbrains.dokka.javadoc.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

class JavadocDocumentableJVMSourceSetFilter(val context: DokkaContext) : PreMergeDocumentableTransformer {

    private val allowedSourceSets = context.configuration.sourceSets.filter { it.analysisPlatform == Platform.jvm }
        .flatMap { it.getAllDependentSourceSets() }.distinct()

    private fun DokkaConfiguration.DokkaSourceSet.getAllDependentSourceSets(): List<DokkaConfiguration.DokkaSourceSet> =
        dependentSourceSets.flatMap { setId ->
            context.configuration.sourceSets.find { it.sourceSetID == setId }?.getAllDependentSourceSets().orEmpty()
        } + this

    override fun invoke(modules: List<DModule>): List<DModule> =
        modules.filter { module -> allowedSourceSets.containsAll(module.sourceSets) }
}