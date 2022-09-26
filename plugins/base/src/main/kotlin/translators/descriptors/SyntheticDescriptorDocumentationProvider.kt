package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.base.parsers.MarkdownParser
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.resolve.DescriptorFactory

private const val ENUM_VALUEOF_TEMPLATE_PATH = "/dokka/docs/kdoc/EnumValueOf.kt.template"
private const val ENUM_VALUES_TEMPLATE_PATH = "/dokka/docs/kdoc/EnumValues.kt.template"

 internal class SyntheticDescriptorDocumentationProvider(
    private val resolutionFacade: DokkaResolutionFacade
) {
    fun isDocumented(descriptor: DeclarationDescriptor): Boolean = descriptor is FunctionDescriptor
            && (DescriptorFactory.isEnumValuesMethod(descriptor) || DescriptorFactory.isEnumValueOfMethod(descriptor))

    fun getDocumentation(descriptor: DeclarationDescriptor): DocumentationNode? {
        val function = descriptor as? FunctionDescriptor ?: return null
        return when {
            DescriptorFactory.isEnumValuesMethod(function) -> loadTemplate(descriptor, ENUM_VALUES_TEMPLATE_PATH)
            DescriptorFactory.isEnumValueOfMethod(function) -> loadTemplate(descriptor, ENUM_VALUEOF_TEMPLATE_PATH)
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
        resolveKDocLink(
            resolutionFacade.resolveSession.bindingContext,
            resolutionFacade,
            descriptor,
            null,
            link.split('.')
        ).firstOrNull()?.let { DRI.from(it) }
}
