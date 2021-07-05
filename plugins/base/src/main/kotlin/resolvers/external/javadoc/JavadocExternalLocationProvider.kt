package org.jetbrains.dokka.base.resolvers.external.javadoc

import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.htmlEscape

open class JavadocExternalLocationProvider(
        externalDocumentation: ExternalDocumentation,
        val brackets: String,
        val separator: String,
        dokkaContext: DokkaContext
) : DefaultExternalLocationProvider(externalDocumentation, ".html", dokkaContext) {

    override fun DRI.constructPath(): String {
        val packageLink = packageName?.replace(".", "/")
        val modulePart = packageName?.let { packageName ->
            externalDocumentation.packageList.moduleFor(packageName)?.let {
                if (it.isNotBlank())
                    "$it/"
                else
                    ""
            }
        }.orEmpty()

        val docWithModule = docURL + modulePart

        if (classNames == null) {
            return "$docWithModule$packageLink/package-summary$extension".htmlEscape()
        }
        val classLink =
                if (packageLink == null) "${classNames}$extension" else "$packageLink/${classNames}$extension"
        val callableChecked = callable ?: return "$docWithModule$classLink".htmlEscape()

        return ("$docWithModule$classLink#" + anchorPart(callableChecked)).htmlEscape()
    }

    protected open fun anchorPart(callable: Callable) = callable.name +
            "${brackets.first()}" +
            callable.params.joinToString(separator) +
            "${brackets.last()}"

}
