package org.jetbrains.dokka

import com.google.inject.Inject
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade

class DeclarationLinkResolver
        @Inject constructor(val resolutionFacade: ResolutionFacade,
                            val refGraph: NodeReferenceGraph,
                            val logger: DokkaLogger) {
    fun resolveContentLink(fromDescriptor: DeclarationDescriptor, href: String): ContentBlock {
        val symbol = try {
            val symbols = resolveKDocLink(resolutionFacade, fromDescriptor, null, href.split('.').toList())
            findTargetSymbol(symbols)
        } catch(e: Exception) {
            null
        }

        // don't include unresolved links in generated doc
        // assume that if an href doesn't contain '/', it's not an attempt to reference an external file
        if (symbol != null) {
            return ContentNodeLazyLink(href, { -> refGraph.lookup(symbol.signature()) })
        }
        if ("/" in href) {
            return ContentExternalLink(href)
        }
        logger.warn("Unresolved link to $href in doc comment of ${fromDescriptor.signatureWithSourceLocation()}")
        return ContentExternalLink("#")
    }

    fun findTargetSymbol(symbols: Collection<DeclarationDescriptor>): DeclarationDescriptor? {
        if (symbols.isEmpty()) {
            return null
        }
        val symbol = symbols.first()
        if (symbol is CallableMemberDescriptor && symbol.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return symbol.overriddenDescriptors.firstOrNull()
        }
        return symbol
    }

}