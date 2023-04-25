package org.jetbrains.dokka.base.parsers.moduleAndPackage

import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.base.parsers.MarkdownParser
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentation.Classifier.Module
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentation.Classifier.Package
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

fun interface ModuleAndPackageDocumentationParsingContext {
    fun markdownParserFor(fragment: ModuleAndPackageDocumentationFragment, location: String): MarkdownParser
}

internal fun ModuleAndPackageDocumentationParsingContext.parse(
    fragment: ModuleAndPackageDocumentationFragment
): DocumentationNode {
    return markdownParserFor(fragment, fragment.source.sourceDescription).parse(fragment.documentation)
}

fun ModuleAndPackageDocumentationParsingContext(
    logger: DokkaLogger,
    facade: DokkaResolutionFacade? = null
) = ModuleAndPackageDocumentationParsingContext { fragment, sourceLocation ->
    val descriptor = when (fragment.classifier) {
        Module -> facade?.moduleDescriptor?.getPackage(FqName.topLevel(Name.identifier("")))
        Package -> facade?.moduleDescriptor?.getPackage(FqName(fragment.name))
    }

    val externalDri = { link: String ->
        try {
            if (facade != null && descriptor != null) {
                resolveKDocLink(
                    facade.resolveSession.bindingContext,
                    facade,
                    descriptor,
                    null,
                    link.split('.')
                ).sorted().firstOrNull()?.let { DRI.from(it) }
            } else null
        } catch (e1: IllegalArgumentException) {
            logger.warn("Couldn't resolve link for $link")
            null
        }
    }

    MarkdownParser(externalDri = externalDri, sourceLocation)
}

private fun Collection<DeclarationDescriptor>.sorted() = sortedWith(
    compareBy(
        { it is ClassDescriptor },
        { (it as? FunctionDescriptor)?.name },
        { (it as? FunctionDescriptor)?.valueParameters?.size },
        { (it as? FunctionDescriptor)?.valueParameters?.joinToString { it.type.toString() } }
    )
)
