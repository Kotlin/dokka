package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation

fun interface ExternalLocationProviderFactory {
    fun getExternalLocationProvider(doc: ExternalDocumentation): ExternalLocationProvider?
}
