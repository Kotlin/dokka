package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider.Companion.identifierToFilename
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext

open class Dokka010ExternalLocationProvider(
    val externalDocumentation: ExternalDocumentation,
    val extension: String,
    val dokkaContext: DokkaContext
) : ExternalLocationProvider {
    override fun resolve(dri: DRI): String? {
        val docURL = externalDocumentation.documentationURL.toString().removeSuffix("/") + "/"

        val fqName = listOfNotNull(dri.packageName.takeIf { it?.isNotBlank() == true },
            dri.classNames.takeIf { it?.isNotBlank() == true }?.removeCompanion()).joinToString(".")
        val relocationId =
            fqName.let { if (dri.callable != null) it + "$" + dri.callable!!.toOldString() else it }
        externalDocumentation.packageList.locations[relocationId]?.let { path -> return "$docURL$path" }

        val classNamesChecked = dri.classNames?.removeCompanion()
            ?: return "$docURL${dri.packageName ?: ""}/index$extension"

        val classLink = (listOfNotNull(dri.packageName) + classNamesChecked.split('.'))
            .joinToString("/", transform = ::identifierToFilename)

        val callableChecked = dri.callable ?: return "$docURL$classLink/index$extension"
        return "$docURL$classLink/" + identifierToFilename(callableChecked.name) + extension
    }
    private fun String.removeCompanion() = removeSuffix(".Companion")

    private fun Callable.toOldString() = name + params.joinToString(", ", "(", ")") + (receiver?.let { "#$it" } ?: "")
}
