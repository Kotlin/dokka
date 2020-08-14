package org.jetbrains.dokka.base.resolvers.external.javadoc

import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.htmlEscape

class JavadocExternalLocationProvider(
    externalDocumentation: ExternalDocumentation,
    val brackets: String,
    val separator: String,
    dokkaContext: DokkaContext
) : DefaultExternalLocationProvider(externalDocumentation, ".html", dokkaContext) {

    override fun resolve(dri: DRI): String? {
        val docURL = externalDocumentation.documentationURL.toString().removeSuffix("/") + "/"
        val packageLink = dri.packageName?.replace(".", "/")
        if (dri.classNames == null) {
            return "$docURL$packageLink/package-summary$extension".htmlEscape()
        }
        val classLink = if (packageLink == null) "${dri.classNames}$extension" else "$packageLink/${dri.classNames}$extension"
        val callableChecked = dri.callable ?: return "$docURL$classLink".htmlEscape()

        return ("$docURL$classLink#" +
                callableChecked.name +
                "${brackets.first()}" +
                callableChecked.params.joinToString(separator) +
                "${brackets.last()}").htmlEscape()
    }
}
