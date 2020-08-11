package org.jetbrains.dokka.base.resolvers.external.javadoc

import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProviderFactoryWithCache
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentationInfo
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.plugability.DokkaContext

class JavadocExternalLocationProviderFactory(val context: DokkaContext) :
    ExternalLocationProviderFactory by ExternalLocationProviderFactoryWithCache(
        object : ExternalLocationProviderFactory {
            override fun getExternalLocationProvider(docInfo: ExternalDocumentationInfo): ExternalLocationProvider? =
                when (docInfo.packageList.linkFormat) {
                    RecognizedLinkFormat.Javadoc1 ->
                        JavadocExternalLocationProvider(docInfo, "()", ", ", context) // Covers JDK 1 - 7
                    RecognizedLinkFormat.Javadoc8,
                    RecognizedLinkFormat.DokkaJavadoc ->
                        JavadocExternalLocationProvider(docInfo, "--", "-", context) // Covers JDK 8 - 9
                    RecognizedLinkFormat.Javadoc10 ->
                        JavadocExternalLocationProvider(docInfo, "()", ",", context) // Covers JDK 10
                    else -> null
                }
        }
    )