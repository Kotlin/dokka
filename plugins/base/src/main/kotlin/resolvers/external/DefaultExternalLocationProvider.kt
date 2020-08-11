package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider.Companion.identifierToFilename
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentationInfo
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext

open class DefaultExternalLocationProvider(
    val externalDocumentationInfo: ExternalDocumentationInfo,
    val extension: String,
    val dokkaContext: DokkaContext
) : ExternalLocationProvider {
    override fun resolve(dri: DRI): String? { // TODO: classes without packages?
        val docURL = externalDocumentationInfo.documentationURL.toString().removeSuffix("/") + "/"
        val classNamesChecked = dri.classNames ?: return "$docURL${dri.packageName ?: ""}/index$extension"
        val classLink = (listOfNotNull(dri.packageName) + classNamesChecked.split('.'))
            .joinToString("/", transform = ::identifierToFilename)

        val callableChecked = dri.callable ?: return "$docURL$classLink/index$extension"
        return "$docURL$classLink/" + identifierToFilename(callableChecked.name) + extension
    }
}
