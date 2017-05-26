package org.jetbrains.dokka

import com.google.inject.Inject
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import java.net.URL


class ExternalDocumentationLinkResolver @Inject constructor(
        val options: DocumentationOptions
) {

    val packageFqNameToLocation = mutableMapOf<FqName, ExternalDocumentationRoot>()
    val formats = mutableMapOf<String, InboundExternalLinkResolutionService>()

    class ExternalDocumentationRoot(val rootUrl: URL, val resolver: InboundExternalLinkResolutionService, val locations: Map<String, String>)

    fun loadPackageLists() {
        options.externalDocumentationLinks.forEach { link ->
            val linkUrl = URL(link.url)
            val packageListUrl = link.packageListUrl?.let { URL(it) } ?: URL(linkUrl, "package-list")
            val (params, packages) =
                    packageListUrl
                            .openStream()
                            .bufferedReader()
                            .useLines { lines -> lines.partition { it.startsWith(DOKKA_PARAM_PREFIX) } }

            val paramsMap = params.asSequence()
                    .map { it.removePrefix(DOKKA_PARAM_PREFIX).split(":", limit = 2) }
                    .groupBy({ (key, _) -> key }, { (_, value) -> value })

            val format = paramsMap["format"]?.singleOrNull() ?: "javadoc"

            val locations = paramsMap["location"].orEmpty()
                    .map { it.split("\u001f", limit = 2) }
                    .map { (key, value) -> key to value }
                    .toMap()

            val resolver = if (format == "javadoc") {
                InboundExternalLinkResolutionService.Javadoc()
            } else {
                val linkExtension = paramsMap["linkExtension"]?.singleOrNull() ?:
                        throw RuntimeException("Failed to parse package list from ${link.packageListUrl}")
                InboundExternalLinkResolutionService.Dokka(linkExtension)
            }

            val rootInfo = ExternalDocumentationRoot(linkUrl, resolver, locations)

            packages.map { FqName(it) }.forEach { packageFqNameToLocation[it] = rootInfo }
        }
    }

    init {
        loadPackageLists()
    }

    fun buildExternalDocumentationLink(symbol: DeclarationDescriptor): String? {
        val packageFqName: FqName =
                when (symbol) {
                    is DeclarationDescriptorNonRoot -> symbol.parents.firstOrNull { it is PackageFragmentDescriptor }?.fqNameSafe ?: return null
                    is PackageFragmentDescriptor -> symbol.fqName
                    else -> return null
                }

        val externalLocation = packageFqNameToLocation[packageFqName] ?: return null

        val path = externalLocation.locations[symbol.signature()] ?:
                externalLocation.resolver.getPath(symbol) ?: return null

        return URL(externalLocation.rootUrl, path).toExternalForm()
    }

    companion object {
        const val DOKKA_PARAM_PREFIX = "\$dokka."
    }
}


interface InboundExternalLinkResolutionService {
    fun getPath(symbol: DeclarationDescriptor): String?

    class Javadoc : InboundExternalLinkResolutionService {
        override fun getPath(symbol: DeclarationDescriptor): String? {
            if (symbol is JavaClassDescriptor) {
                return DescriptorUtils.getFqName(symbol).asString().replace(".", "/") + ".html"
            } else if (symbol is JavaCallableMemberDescriptor) {
                val containingClass = symbol.containingDeclaration as? JavaClassDescriptor ?: return null
                val containingClassLink = getPath(containingClass)
                if (containingClassLink != null) {
                    if (symbol is JavaMethodDescriptor) {
                        val psi = symbol.sourcePsi() as? PsiMethod
                        if (psi != null) {
                            val params = psi.parameterList.parameters.joinToString { it.type.canonicalText }
                            return containingClassLink + "#" + symbol.name + "(" + params + ")"
                        }
                    } else if (symbol is JavaPropertyDescriptor) {
                        return "$containingClassLink#${symbol.name}"
                    }
                }
            }
            // TODO Kotlin javadoc
            return null
        }
    }

    class Dokka(val extension: String) : InboundExternalLinkResolutionService {
        override fun getPath(symbol: DeclarationDescriptor): String? {
            val leafElement = when (symbol) {
                is CallableDescriptor, is TypeAliasDescriptor -> true
                else -> false
            }
            val path = getPathWithoutExtension(symbol)
            if (leafElement) return "$path.$extension"
            else return "$path/index.$extension"
        }

        fun getPathWithoutExtension(symbol: DeclarationDescriptor): String {
            if (symbol.containingDeclaration == null)
                return identifierToFilename(symbol.name.asString())
            else if (symbol is PackageFragmentDescriptor) {
                return symbol.fqName.asString()
            } else {
                return getPathWithoutExtension(symbol.containingDeclaration!!) + '/' + identifierToFilename(symbol.name.asString())
            }
        }

    }
}

