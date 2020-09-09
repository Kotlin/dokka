package org.jetbrains.dokka.location.external

import org.jetbrains.dokka.location.shared.ExternalDocumentation
import java.util.concurrent.ConcurrentHashMap

class ExternalLocationProviderFactoryWithCache(val ext: ExternalLocationProviderFactory) :
    ExternalLocationProviderFactory {

    private val locationProviders = ConcurrentHashMap<ExternalDocumentation, CacheWrapper>()

    override fun getExternalLocationProvider(doc: ExternalDocumentation): ExternalLocationProvider? =
        locationProviders.getOrPut(doc) { CacheWrapper(ext.getExternalLocationProvider(doc)) }.provider

    private class CacheWrapper(val provider: ExternalLocationProvider?)
}

