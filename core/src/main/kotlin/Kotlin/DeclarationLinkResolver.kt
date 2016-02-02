package org.jetbrains.dokka

import com.google.inject.Inject
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

class DeclarationLinkResolver
        @Inject constructor(val resolutionFacade: DokkaResolutionFacade,
                            val refGraph: NodeReferenceGraph,
                            val logger: DokkaLogger,
                            val options: DocumentationOptions) {
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
            val jdkHref = buildJdkLink(symbol)
            if (jdkHref != null) {
                return ContentExternalLink(jdkHref)
            }
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

    fun buildJdkLink(symbol: DeclarationDescriptor): String? {
        if (symbol is JavaClassDescriptor) {
            val fqName = DescriptorUtils.getFqName(symbol)
            if (fqName.startsWith(Name.identifier("java")) || fqName.startsWith(Name.identifier("javax"))) {
                return javadocRoot + fqName.asString().replace(".", "/") + ".html"
            }
        }
        else if (symbol is JavaMethodDescriptor) {
            val containingClass = symbol.containingDeclaration as? JavaClassDescriptor ?: return null
            val containingClassLink = buildJdkLink(containingClass)
            if (containingClassLink != null) {
                val psi = symbol.sourcePsi() as? PsiMethod
                if (psi != null) {
                    val params = psi.parameterList.parameters.joinToString { it.type.canonicalText }
                    return containingClassLink + "#" + symbol.name + "(" + params + ")"
                }
            }
        }
        return null
    }

    private val javadocRoot = "http://docs.oracle.com/javase/${options.jdkVersion}/docs/api/"
}
