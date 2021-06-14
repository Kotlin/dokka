package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider.Companion.identifierToFilename
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext

open class MultiModuleExternalLocationProvider(
    val externalDocumentation: ExternalDocumentation,
    val extension: String,
    val dokkaContext: DokkaContext
) : ExternalLocationProvider {
    protected val docURL = externalDocumentation.documentationURL.toString().removeSuffix("/") + "/"

    override fun resolve(dri: DRI): String? {
        externalDocumentation.packageList.locations[dri.toString()]?.let { path -> return "$docURL$path" }

        if (dri.packageName !in externalDocumentation.packageList.packages)
            return null

        return dri.constructPath()
    }

    protected open fun DRI.constructPath(): String {
        val module = externalDocumentation
                .packageList
                .modules
                .filter { it.value.contains(packageName) }
                .keys
                .firstOrNull()
        val classNamesChecked = classNames ?: return "$docURL/$module/${packageName ?: ""}/index$extension"
        val classLink = (listOfNotNull(packageName) + classNamesChecked.split('.'))
                .joinToString("/", transform = ::identifierToFilename)

        val fileName = callable?.let { identifierToFilename(it.name) } ?: "index"
        return "$docURL$classLink/$fileName$extension"
    }
}
