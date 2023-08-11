package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Module
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentation.Classifier.Package
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.resolveKDocLink
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromSymbol
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.FqName

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
        val analysisContext = kotlinAnalysis[sourceSet]
        analyze(analysisContext.mainModule) {
            val contextSymbol = when (fragment.classifier) {
                Module -> ROOT_PACKAGE_SYMBOL
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
