package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentationInfo
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.plugability.DokkaContext

class DefaultExternalLocationProviderFactory(val context: DokkaContext) :
    ExternalLocationProviderFactory by ExternalLocationProviderFactoryWithCache(
        object : ExternalLocationProviderFactory {
            override fun getExternalLocationProvider(docInfo: ExternalDocumentationInfo): ExternalLocationProvider? =
                when (docInfo.packageList.linkFormat) {
                    RecognizedLinkFormat.KotlinWebsiteHtml,
                    RecognizedLinkFormat.DokkaOldHtml,
                    RecognizedLinkFormat.DokkaHtml -> DefaultExternalLocationProvider(docInfo, ".html", context)
                    RecognizedLinkFormat.DokkaGFM,
                    RecognizedLinkFormat.DokkaJekyll -> DefaultExternalLocationProvider(docInfo, ".md", context)
                    else -> null
                }
        }
    )