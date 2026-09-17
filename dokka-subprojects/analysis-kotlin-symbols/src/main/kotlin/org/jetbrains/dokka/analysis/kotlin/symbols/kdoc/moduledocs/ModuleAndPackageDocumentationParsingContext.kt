/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Module
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Package
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.resolveKDocTextLink
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromSymbol
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.KotlinAnalysis
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.contextModule
import org.jetbrains.kotlin.analysis.api.resolution.resolveSymbols
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteModule
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtPsiFactory

internal fun interface ModuleAndPackageDocumentationParsingContext {
    fun markdownParserFor(fragment: ModuleAndPackageDocumentationFragment, location: String): MarkdownParser
}

internal fun ModuleAndPackageDocumentationParsingContext.parse(
    fragment: ModuleAndPackageDocumentationFragment
): DocumentationNode {
    return markdownParserFor(fragment, fragment.source.sourceDescription).parse(fragment.documentation)
}

internal fun ModuleAndPackageDocumentationParsingContext(
    logger: DokkaLogger,
    kotlinAnalysis: KotlinAnalysis? = null,
    sourceSet: DokkaConfiguration.DokkaSourceSet? = null
) = ModuleAndPackageDocumentationParsingContext { fragment, sourceLocation ->

    if (kotlinAnalysis == null || sourceSet == null) {
        MarkdownParser(externalDri = { null }, sourceLocation)
    } else {
        val sourceModule = kotlinAnalysis.getModule(sourceSet)
        val contextPackageFQN = when (fragment.classifier) {
            Module -> null
            Package -> fragment.name
        }
        val locationInformation = when (fragment.classifier) {
            Module -> "module documentation"
            Package -> "'${fragment.name}' package documentation"
        }
        MarkdownParser(
            externalDri = { link ->
                analyze(sourceModule) {
                    resolveModuleDocumentationTextLink(link, contextPackageFQN, locationInformation, logger, sourceSet)
                }
            },
            sourceLocation
        )
    }
}

context(_: KaSession)
private fun resolveModuleDocumentationTextLink(
    link: String,
    contextPackageFQN: String?,
    locationInformation: String,
    logger: DokkaLogger,
    sourceSet: DokkaConfiguration.DokkaSourceSet
): DRI? {
    val dri = resolveKDocTextLink(link, contextPackageFQN, locationInformation, logger, sourceSet)
    return if (dri?.isPackageLink() == true) {
        resolveTopLevelCallableLink(link, contextPackageFQN) ?: dri
    } else {
        dri
    }
}

private fun DRI.isPackageLink(): Boolean = packageName != null && classNames == null && callable == null

context(_: KaSession)
private fun resolveTopLevelCallableLink(link: String, contextPackageFQN: String?): DRI? {
    val kDocLink = createKDocLink(link, contextPackageFQN) ?: return null
    return analyze(kDocLink) {
        kDocLink.children.filterIsInstance<KDocName>().lastOrNull()
            ?.resolveSymbols()
            ?.asSequence()
            ?.filter { it is KaFunctionSymbol || it is KaVariableSymbol }
            ?.map(::getDRIFromSymbol)
            ?.distinct()
            ?.take(2)
            ?.toList()
            ?.singleOrNull()
    }
}

context(_: KaSession)
private fun createKDocLink(link: String, contextPackageFQN: String?): KDocLink? {
    val currentModule: KaSourceModule = useSiteModule as? KaSourceModule ?: return null

    val dummyFileText = if (!contextPackageFQN.isNullOrBlank()) """
    package $contextPackageFQN

    /**
    * [$link]
    */
    """.trimIndent()
    else """
    /**
    * [$link]
    */
    """

    val dummyFile = KtPsiFactory(currentModule.project).createFile(dummyFileText)

    @OptIn(KaExperimentalApi::class)
    dummyFile.contextModule = currentModule

    val kDoc = dummyFile.children.filterIsInstance<KDoc>().single()
    return kDoc.getDefaultSection().children.filterIsInstance<KDocLink>().singleOrNull()
}
