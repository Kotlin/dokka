package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentationInfo

interface ExternalLocationProviderFactory {
    fun getExternalLocationProvider(docInfo: ExternalDocumentationInfo): ExternalLocationProvider?
}