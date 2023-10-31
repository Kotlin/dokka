/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.external.javadoc

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.androidSdk
import org.jetbrains.dokka.androidX
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProviderFactoryWithCache
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.plugability.DokkaContext

public class JavadocExternalLocationProviderFactory(
    public val context: DokkaContext,
) : ExternalLocationProviderFactory by ExternalLocationProviderFactoryWithCache(
    { doc ->
        when (doc.packageList.url) {
            DokkaConfiguration.ExternalDocumentationLink.androidX().packageListUrl,
            DokkaConfiguration.ExternalDocumentationLink.androidSdk().packageListUrl,
            ->
                AndroidExternalLocationProvider(doc, context)

            else ->
                when (doc.packageList.linkFormat) {
                    RecognizedLinkFormat.Javadoc1 ->
                        JavadocExternalLocationProvider(doc, "()", ", ", context) // Covers JDK 1 - 7
                    RecognizedLinkFormat.Javadoc8 ->
                        JavadocExternalLocationProvider(doc, "--", "-", context) // Covers JDK 8 - 9
                    RecognizedLinkFormat.Javadoc10,
                    RecognizedLinkFormat.DokkaJavadoc,
                    ->
                        JavadocExternalLocationProvider(doc, "()", ",", context) // Covers JDK 10
                    else -> null
                }
        }
    }
)
