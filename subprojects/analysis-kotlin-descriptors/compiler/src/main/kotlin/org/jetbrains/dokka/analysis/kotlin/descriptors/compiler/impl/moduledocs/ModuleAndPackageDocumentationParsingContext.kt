/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.moduledocs

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.KDocFinder
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.from
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.moduledocs.ModuleAndPackageDocumentation.Classifier.Module
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.moduledocs.ModuleAndPackageDocumentation.Classifier.Package
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

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
    moduleDescriptor: ModuleDescriptor? = null,
    kDocFinder: KDocFinder? = null,
    sourceSet: DokkaConfiguration.DokkaSourceSet? = null
) = ModuleAndPackageDocumentationParsingContext { fragment, sourceLocation ->
    val descriptor = when (fragment.classifier) {
        Module -> moduleDescriptor?.getPackage(FqName.topLevel(Name.identifier("")))
        Package -> moduleDescriptor?.getPackage(FqName(fragment.name))
    }

    val externalDri = { link: String ->
        try {
            if (kDocFinder != null && descriptor != null && sourceSet != null) {
                with(kDocFinder) {
                    resolveKDocLink(
                        descriptor,
                        link,
                        sourceSet
                    ).sorted().firstOrNull()?.let {
                        DRI.from(
                            it
                        )
                    }
                }
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
