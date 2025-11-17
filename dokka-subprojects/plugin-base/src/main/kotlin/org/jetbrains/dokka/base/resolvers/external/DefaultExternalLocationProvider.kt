/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider.Companion.identifierToFilename
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext

public open class DefaultExternalLocationProvider(
    public val externalDocumentation: ExternalDocumentation,
    public val extension: String,
    public val dokkaContext: DokkaContext
) : ExternalLocationProvider {
    public val docURL: String = externalDocumentation.documentationURL.toString().removeSuffix("/") + "/"

    override fun resolve(dri: DRI): String? {
        externalDocumentation.packageList.locations[dri.toString()]?.let { path -> return "$docURL$path" }

        if (dri.packageName !in externalDocumentation.packageList.packages)
            return null

        return dri.constructPath()
    }

    protected open fun DRI.constructPath(): String {
        val modulePart = packageName?.let { packageName ->
            externalDocumentation.packageList.moduleFor(packageName)?.let {
                if (it.isNotBlank())
                    "$it/"
                else
                    ""
            }
        }.orEmpty()

        val docWithModule = docURL + modulePart
        val classNamesChecked = classNames ?: return "$docWithModule${packageName ?: ""}/index$extension"
        val classLink = (listOfNotNull(packageName) + classNamesChecked.split('.'))
            .joinToString("/", transform = ::identifierToFilename)

        val fileName = callable?.let { identifierToFilename(it.name) } ?: "index"
        return "$docWithModule$classLink/$fileName$extension"
    }
}
