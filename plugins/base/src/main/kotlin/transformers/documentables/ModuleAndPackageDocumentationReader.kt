@file:Suppress("FunctionName")

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.KotlinAnalysis
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentation.Classifier
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentationFragment
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentationParsingContext
import org.jetbrains.dokka.base.parsers.moduleAndPackage.parseModuleAndPackageDocumentation
import org.jetbrains.dokka.base.parsers.moduleAndPackage.parseModuleAndPackageDocumentationFragments
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.associateWithNotNull

internal interface ModuleAndPackageDocumentationReader {
    operator fun get(module: DModule): SourceSetDependent<DocumentationNode>
    operator fun get(pkg: DPackage): SourceSetDependent<DocumentationNode>
}

internal fun ModuleAndPackageDocumentationReader(
    context: DokkaContext, kotlinAnalysis: KotlinAnalysis? = null
): ModuleAndPackageDocumentationReader = ContextModuleAndPackageDocumentationReader(context, kotlinAnalysis)

private class ContextModuleAndPackageDocumentationReader(
    private val context: DokkaContext,
    private val kotlinAnalysis: KotlinAnalysis?
) : ModuleAndPackageDocumentationReader {

    private val documentationFragments: SourceSetDependent<List<ModuleAndPackageDocumentationFragment>> =
        context.configuration.sourceSets.associateWith { sourceSet ->
            sourceSet.includes.flatMap { include -> parseModuleAndPackageDocumentationFragments(include) }
        }

    private fun findDocumentationNodes(
        sourceSets: Set<DokkaConfiguration.DokkaSourceSet>,
        predicate: (ModuleAndPackageDocumentationFragment) -> Boolean
    ): SourceSetDependent<DocumentationNode> {
        return sourceSets.associateWithNotNull { sourceSet ->
            val fragments = documentationFragments[sourceSet].orEmpty().filter(predicate)
            val resolutionFacade = kotlinAnalysis?.get(sourceSet)?.facade
            val documentations = fragments.map { fragment ->
                parseModuleAndPackageDocumentation(
                    context = ModuleAndPackageDocumentationParsingContext(context.logger, resolutionFacade),
                    fragment = fragment
                )
            }
            when (documentations.size) {
                0 -> null
                1 -> documentations.single().documentation
                else -> DocumentationNode(documentations.flatMap { it.documentation.children })
            }
        }
    }

    private val ModuleAndPackageDocumentationFragment.canonicalPackageName: String
        get() {
            check(classifier == Classifier.Package)
            if (name == "[root]") return ""
            return name
        }

    override fun get(module: DModule): SourceSetDependent<DocumentationNode> {
        return findDocumentationNodes(module.sourceSets) { fragment ->
            fragment.classifier == Classifier.Module && (fragment.name == module.name)
        }
    }

    override fun get(pkg: DPackage): SourceSetDependent<DocumentationNode> {
        return findDocumentationNodes(pkg.sourceSets) { fragment ->
            fragment.classifier == Classifier.Package && fragment.canonicalPackageName == pkg.dri.packageName
        }
    }
}
