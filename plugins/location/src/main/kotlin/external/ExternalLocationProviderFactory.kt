package org.jetbrains.dokka.location.external

import org.jetbrains.dokka.location.shared.ExternalDocumentation

interface ExternalLocationProviderFactory {
    fun getExternalLocationProvider(doc: ExternalDocumentation): ExternalLocationProvider?
}
