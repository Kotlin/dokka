package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider.Companion.identifierToFilename
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaBaseContext
import org.jetbrains.dokka.plugability.DokkaContext

open class DefaultExternalLocationProvider(
    val externalDocumentation: ExternalDocumentation,
    val extension: String,
    val dokkaContext: DokkaBaseContext
) : ExternalLocationProvider {
    override fun resolve(dri: DRI): String? {
        val docURL = externalDocumentation.documentationURL.toString().removeSuffix("/") + "/"
        externalDocumentation.packageList.locations[dri.toString()]?.let { path -> return "$docURL$path" }

        val classNamesChecked = dri.classNames ?: return "$docURL${dri.packageName ?: ""}/index$extension"
        val classLink = (listOfNotNull(dri.packageName) + classNamesChecked.split('.'))
            .joinToString("/", transform = ::identifierToFilename)

        val callableChecked = dri.callable ?: return "$docURL$classLink/index$extension"
        return "$docURL$classLink/" + identifierToFilename(callableChecked.name) + extension
    }
}
