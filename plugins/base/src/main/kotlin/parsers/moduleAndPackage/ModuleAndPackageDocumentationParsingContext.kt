@file:Suppress("FunctionName")

package org.jetbrains.dokka.base.parsers.moduleAndPackage

import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.base.parsers.MarkdownParser
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentation.Classifier.*
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun interface ModuleAndPackageDocumentationParsingContext {
    fun markdownParserFor(fragment: ModuleAndPackageDocumentationFragment): MarkdownParser
}

internal fun ModuleAndPackageDocumentationParsingContext.parse(
    fragment: ModuleAndPackageDocumentationFragment
): DocumentationNode {
    return markdownParserFor(fragment).parse(fragment.documentation)
}

internal fun ModuleAndPackageDocumentationParsingContext(
    logger: DokkaLogger,
    facade: DokkaResolutionFacade? = null,
) = ModuleAndPackageDocumentationParsingContext { fragment ->
    val descriptor = when (fragment.classifier) {
        Module -> facade?.moduleDescriptor?.getPackage(FqName.topLevel(Name.identifier("")))
        Package -> facade?.moduleDescriptor?.getPackage(FqName(fragment.name))
    }
    MarkdownParser(
        resolutionFacade = facade,
        declarationDescriptor = descriptor,
        logger = logger
    )
}
