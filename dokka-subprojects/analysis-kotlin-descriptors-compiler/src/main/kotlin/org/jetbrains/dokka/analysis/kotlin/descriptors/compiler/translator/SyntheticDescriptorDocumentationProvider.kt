/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.translator

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.KDocFinder
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.from
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils

private const val ENUM_ENTRIES_TEMPLATE_PATH = "/dokka/docs/kdoc/EnumEntries.kt.template"
private const val ENUM_VALUEOF_TEMPLATE_PATH = "/dokka/docs/kdoc/EnumValueOf.kt.template"
private const val ENUM_VALUES_TEMPLATE_PATH = "/dokka/docs/kdoc/EnumValues.kt.template"

internal class SyntheticDescriptorDocumentationProvider(
    private val kDocFinder: KDocFinder,
    private val sourceSet: DokkaConfiguration.DokkaSourceSet
) {
    fun isDocumented(descriptor: DeclarationDescriptor): Boolean {
        return when(descriptor) {
            is PropertyDescriptor -> descriptor.isEnumEntries()
            is FunctionDescriptor -> {
                DescriptorFactory.isEnumValuesMethod(descriptor) || DescriptorFactory.isEnumValueOfMethod(descriptor)
            }
            else -> false
        }
    }

    private fun PropertyDescriptor.isEnumEntries(): Boolean {
        return this.name == StandardNames.ENUM_ENTRIES
                && this.kind == CallableMemberDescriptor.Kind.SYNTHESIZED
                && DescriptorUtils.isEnumClass(this.containingDeclaration)
    }

    fun getDocumentation(descriptor: DeclarationDescriptor): DocumentationNode? {
        return when(descriptor) {
            is PropertyDescriptor -> descriptor.getDocumentation()
            is FunctionDescriptor -> descriptor.getDocumentation()
            else -> null
        }
    }

    private fun PropertyDescriptor.getDocumentation(): DocumentationNode? {
        return when {
            this.isEnumEntries() -> loadTemplate(this, ENUM_ENTRIES_TEMPLATE_PATH)
            else -> null
        }
    }

    private fun FunctionDescriptor.getDocumentation(): DocumentationNode? {
        return when {
            DescriptorFactory.isEnumValuesMethod(this) -> loadTemplate(this, ENUM_VALUES_TEMPLATE_PATH)
            DescriptorFactory.isEnumValueOfMethod(this) -> loadTemplate(this, ENUM_VALUEOF_TEMPLATE_PATH)
            else -> null
        }
    }

    private fun loadTemplate(descriptor: DeclarationDescriptor, filePath: String): DocumentationNode? {
        val kdoc = loadContent(filePath) ?: return null
        val parser = MarkdownParser({ link -> resolveLink(descriptor, link)}, filePath)
        return parser.parse(kdoc)
    }

    private fun loadContent(filePath: String): String? = javaClass.getResource(filePath)?.readText()

    private fun resolveLink(descriptor: DeclarationDescriptor, link: String): DRI? =
        kDocFinder.resolveKDocLink(
            fromDescriptor = descriptor,
            qualifiedName = link,
            sourceSet = sourceSet
        ).firstOrNull()?.let { DRI.from(it) }
}
