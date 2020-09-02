package org.jetbrains.dokka.base.resolvers.external.javadoc

import org.jetbrains.dokka.DokkaSourceSet
import org.jetbrains.dokka.androidSdk
import org.jetbrains.dokka.androidX
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProviderFactoryWithCache
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.plugability.DokkaModuleContext
import org.jetbrains.dokka.DokkaSourceSet.ExternalDocumentationLink

class JavadocExternalLocationProviderFactory(val context: DokkaModuleContext) :
    ExternalLocationProviderFactory by ExternalLocationProviderFactoryWithCache(
        object : ExternalLocationProviderFactory {
            override fun getExternalLocationProvider(doc: ExternalDocumentation): ExternalLocationProvider? =
                when (doc.packageList.url) {
                    ExternalDocumentationLink.androidX().packageListUrl,
                    ExternalDocumentationLink.androidSdk().packageListUrl ->
                        AndroidExternalLocationProvider(doc, context)
                    else ->
                        when (doc.packageList.linkFormat) {
                            RecognizedLinkFormat.Javadoc1 ->
                                JavadocExternalLocationProvider(doc, "()", ", ", context) // Covers JDK 1 - 7
                            RecognizedLinkFormat.Javadoc8 ->
                                JavadocExternalLocationProvider(doc, "--", "-", context) // Covers JDK 8 - 9
                            RecognizedLinkFormat.Javadoc10,
                            RecognizedLinkFormat.DokkaJavadoc ->
                                JavadocExternalLocationProvider(doc, "()", ",", context) // Covers JDK 10
                            else -> null
                        }
                }
        }
    )
