package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.getPsiFilesFromPaths
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.getSourceFilePaths
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Module
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Package
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.resolveKDocLink
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromSymbol
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.analyze

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

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

    if(kotlinAnalysis == null || sourceSet == null) {
        MarkdownParser(externalDri = { null }, sourceLocation)
    } else {
        // TODO research another ways to get AnalysisSession
        val analysisContext = kotlinAnalysis[sourceSet]
        val someKtFile = getPsiFilesFromPaths<KtFile>(
            analysisContext.project,
            getSourceFilePaths(sourceSet.sourceRoots.map { it.canonicalPath })
        ).firstOrNull()

        if (someKtFile == null)
            MarkdownParser(externalDri = { null }, sourceLocation)
        else
        analyze(someKtFile) {
            val contextSymbol = when (fragment.classifier) {
                Module -> getPackageSymbolIfPackageExists(FqName.topLevel(Name.identifier("")))
                Package -> getPackageSymbolIfPackageExists(FqName(fragment.name))
            }

            val externalDri = { link: String ->
                try {
                    val linkedSymbol = contextSymbol?.let { resolveKDocLink(link, it) }
                    if (linkedSymbol == null) null
                    else getDRIFromSymbol(linkedSymbol)
                } catch (e1: IllegalArgumentException) {
                    logger.warn("Couldn't resolve link for $link")
                    null
                }
            }

            MarkdownParser(externalDri = externalDri, sourceLocation)
        }
    }
}
/*
private fun Collection<DeclarationDescriptor>.sorted() = sortedWith(
    compareBy(
        { it is ClassDescriptor },
        { (it as? FunctionDescriptor)?.name },
        { (it as? FunctionDescriptor)?.valueParameters?.size },
        { (it as? FunctionDescriptor)?.valueParameters?.joinToString { it.type.toString() } }
    )
)
 */
