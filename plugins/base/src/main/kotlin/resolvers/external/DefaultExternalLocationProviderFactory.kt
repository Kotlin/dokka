package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.plugability.DokkaContext

class DefaultExternalLocationProviderFactory(val context: DokkaContext) :
    ExternalLocationProviderFactory by ExternalLocationProviderFactoryWithCache(
        object : ExternalLocationProviderFactory {
            override fun getExternalLocationProvider(doc: ExternalDocumentation): ExternalLocationProvider? =
                when (doc.packageList.linkFormat) {
                    RecognizedLinkFormat.KotlinWebsiteHtml,
                    RecognizedLinkFormat.DokkaOldHtml -> Dokka010ExternalLocationProvider(doc, ".html", context)
                    RecognizedLinkFormat.DokkaHtml -> DefaultExternalLocationProvider(doc, ".html", context)
                    RecognizedLinkFormat.DokkaGFM,
                    RecognizedLinkFormat.DokkaJekyll -> DefaultExternalLocationProvider(doc, ".md", context)
                    else -> null
                }
        }
    )
