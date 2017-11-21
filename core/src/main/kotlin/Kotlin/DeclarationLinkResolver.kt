package org.jetbrains.dokka

import com.google.inject.Inject
import org.jetbrains.dokka.Model.DescriptorSignatureProvider
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPrivateApi
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi

class DeclarationLinkResolver
        @Inject constructor(val resolutionFacade: DokkaResolutionFacade,
                            val refGraph: NodeReferenceGraph,
                            val logger: DokkaLogger,
                            val options: DocumentationOptions,
                            val externalDocumentationLinkResolver: ExternalDocumentationLinkResolver,
                            val descriptorSignatureProvider: DescriptorSignatureProvider) {


    fun tryResolveContentLink(fromDescriptor: DeclarationDescriptor, href: String): ContentBlock? {
        val symbol = try {
            val symbols = resolveKDocLink(resolutionFacade.resolveSession.bindingContext,
                    resolutionFacade, fromDescriptor, null, href.split('.').toList())
            findTargetSymbol(symbols)
        } catch(e: Exception) {
            null
        }

        // don't include unresolved links in generated doc
        // assume that if an href doesn't contain '/', it's not an attempt to reference an external file
        if (symbol != null) {
            val externalHref = externalDocumentationLinkResolver.buildExternalDocumentationLink(symbol)
            if (externalHref != null) {
                return ContentExternalLink(externalHref)
            }
            val signature = descriptorSignatureProvider.signature(symbol)
            val referencedAt = fromDescriptor.signatureWithSourceLocation()

            return ContentNodeLazyLink(href, { ->
                val target = refGraph.lookup(signature)

                if (target == null) {
                    logger.warn("Can't find node by signature $signature, referenced at $referencedAt")
                }
                target
            })
        }
        if ("/" in href) {
            return ContentExternalLink(href)
        }
        return null
    }

    fun resolveContentLink(fromDescriptor: DeclarationDescriptor, href: String) =
            tryResolveContentLink(fromDescriptor, href) ?: run {
                logger.warn("Unresolved link to $href in doc comment of ${fromDescriptor.signatureWithSourceLocation()}")
                ContentExternalLink("#")
            }

    fun findTargetSymbol(symbols: Collection<DeclarationDescriptor>): DeclarationDescriptor? {
        if (symbols.isEmpty()) {
            return null
        }
        val symbol = symbols.first()
        if (symbol is CallableMemberDescriptor && symbol.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return symbol.overriddenDescriptors.firstOrNull()
        }
        if (symbol is TypeAliasDescriptor && !symbol.isDocumented(options)) {
            return symbol.classDescriptor
        }
        return symbol
    }

}
