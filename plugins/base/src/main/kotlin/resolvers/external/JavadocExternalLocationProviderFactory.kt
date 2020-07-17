package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.utilities.htmlEscape

class JavadocExternalLocationProviderFactory : ExternalLocationProviderFactory by ExternalLocationProviderFactoryWithCache(
    object : ExternalLocationProviderFactory {
        override fun getExternalLocationProvider(param: String): ExternalLocationProvider? =
            when(param) {
                "javadoc1"  -> JavadocExternalLocationProvider(param, "()", ", ") // Covers JDK 1 - 7
                "javadoc8"  -> JavadocExternalLocationProvider(param, "--", "-") // Covers JDK 8 - 9
                "javadoc10" -> JavadocExternalLocationProvider(param, "()", ",") // Covers JDK 10
                else -> null
            }
    }
)

class JavadocExternalLocationProvider(override val param: String, val brackets: String, val separator: String) : ExternalLocationProvider {

    override fun DRI.toLocation(): String {

        val packageLink = packageName?.replace(".", "/")
        if (classNames == null) {
            return "$packageLink/package-summary.html".htmlEscape()
        }
        val classLink = if (packageLink == null) "$classNames.html" else "$packageLink/$classNames.html"
        val callableChecked = callable ?: return classLink.htmlEscape()

        val callableLink = "$classLink#" +
                callableChecked.name +
                "${brackets.first()}" +
                callableChecked.params.joinToString(separator) +
                "${brackets.last()}"

        return callableLink.htmlEscape()
    }
}