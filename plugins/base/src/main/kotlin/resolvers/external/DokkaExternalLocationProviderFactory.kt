package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.local.identifierToFilename
import org.jetbrains.dokka.links.DRI
import java.lang.IllegalStateException


class DokkaExternalLocationProviderFactory : ExternalLocationProviderFactoryWithCache() {

    override fun createExternalLocationProvider(param: String): ExternalLocationProvider? =
        when(param) {
            "kotlin-website-html", "html" -> DokkaExternalLocationProvider(param, ".html")
            "markdown" -> DokkaExternalLocationProvider(param, ".md")
            else -> null
        }
}

class DokkaExternalLocationProvider(override val param: String, val extension: String) : ExternalLocationProvider {

    override fun DRI.toLocation(): String { // TODO: classes without packages?

        val classNamesChecked = classNames ?: return "$packageName/index$extension"

        val classLink = if (packageName == null) {
            ""
        } else {
            "$packageName/"
        } + classNamesChecked.split('.').joinToString("/", transform = ::identifierToFilename)

        val callableChecked = callable ?: return "$classLink/index$extension"

        return "$classLink/${identifierToFilename(
            callableChecked.name
        )}$extension"
    }
}
