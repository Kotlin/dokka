/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider.Companion.identifierToFilename
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext

public open class Dokka010ExternalLocationProvider(
    public val externalDocumentation: ExternalDocumentation,
    public val extension: String,
    public val dokkaContext: DokkaContext
) : ExternalLocationProvider {
    public val docURL: String = externalDocumentation.documentationURL.toString().removeSuffix("/") + "/"

    override fun resolve(dri: DRI): String? {

        val fqName = listOfNotNull(
            dri.packageName.takeIf { it?.isNotBlank() == true },
            dri.classNames.takeIf { it?.isNotBlank() == true }?.removeCompanion()
        ).joinToString(".")
        val relocationId =
            fqName.let { if (dri.callable != null) it + "$" + dri.callable!!.toOldString() else it }
        externalDocumentation.packageList.locations[relocationId]?.let { path -> return "$docURL$path" }

        if (dri.packageName !in externalDocumentation.packageList.packages)
            return null

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
