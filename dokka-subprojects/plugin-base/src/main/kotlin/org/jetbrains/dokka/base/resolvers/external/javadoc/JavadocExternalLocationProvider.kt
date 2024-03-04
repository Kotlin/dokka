/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.external.javadoc

import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.htmlEscape

public open class JavadocExternalLocationProvider(
        externalDocumentation: ExternalDocumentation,
        public val brackets: String,
        public val separator: String,
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

        if (DRIExtraContainer(extra)[EnumEntryDRIExtra] != null) {
            val lastIndex = classNames?.lastIndexOf(".") ?: 0
            val (classSplit, enumEntityAnchor) =
                classNames?.substring(0, lastIndex) to classNames?.substring(lastIndex + 1)

            val classLink =
                if (packageLink == null) "${classSplit}$extension" else "$packageLink/${classSplit}$extension"
            return "$docWithModule$classLink#$enumEntityAnchor".htmlEscape()
        }

        val classLink = if (packageLink == null) "${classNames}$extension" else "$packageLink/${classNames}$extension"
        val callableChecked = callable ?: return "$docWithModule$classLink".htmlEscape()

        return ("$docWithModule$classLink#" + anchorPart(callableChecked)).htmlEscape()
    }

    protected open fun anchorPart(callable: Callable): String {
        return callable.name +
                "${brackets.first()}" +
                callable.params.joinToString(separator) { it.toJavadocURL() } +
                "${brackets.last()}"
    }

    private fun TypeReference.toJavadocURL(): String {
        return when (this) {
            is JavaClassReference -> name
            is Nullable -> wrapped.toJavadocURL() // just ignore
            is StarProjection -> "?"

            // TODO #3502
            is TypeConstructor -> fullyQualifiedName +
                    (if (params.isNotEmpty()) "[${params.joinToString(",") { it.toJavadocURL() }}]" else "")
            is TypeParam -> toString()
            is RecursiveType -> "^".repeat(rank + 1)
        }
    }
}
