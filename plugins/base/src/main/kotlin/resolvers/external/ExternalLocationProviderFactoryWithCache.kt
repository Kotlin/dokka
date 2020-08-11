package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentationInfo
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import java.util.concurrent.ConcurrentHashMap

class ExternalLocationProviderFactoryWithCache(val ext: ExternalLocationProviderFactory) :
    ExternalLocationProviderFactory {

    private val locationProviders = ConcurrentHashMap<ExternalDocumentationInfo, CacheWrapper>()

    override fun getExternalLocationProvider(docInfo: ExternalDocumentationInfo): ExternalLocationProvider? =
        locationProviders.getOrPut(docInfo) { CacheWrapper(ext.getExternalLocationProvider(docInfo)) }.provider

    private class CacheWrapper(val provider: ExternalLocationProvider?)
}

