/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.plugability.DokkaContext

public class DefaultExternalLocationProviderFactory(
    public val context: DokkaContext,
) : ExternalLocationProviderFactory by ExternalLocationProviderFactoryWithCache(
    { doc ->
        when (doc.packageList.linkFormat) {
            RecognizedLinkFormat.KotlinWebsite,
            RecognizedLinkFormat.KotlinWebsiteHtml,
            RecognizedLinkFormat.DokkaOldHtml,
            -> Dokka010ExternalLocationProvider(doc, ".html", context)

            RecognizedLinkFormat.DokkaHtml -> DefaultExternalLocationProvider(doc, ".html", context)
            RecognizedLinkFormat.DokkaGFM,
            RecognizedLinkFormat.DokkaJekyll,
            -> DefaultExternalLocationProvider(doc, ".md", context)

            else -> null
        }
    }
)
