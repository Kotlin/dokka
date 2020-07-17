package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.links.DRI
import java.util.concurrent.ConcurrentHashMap


interface ExternalLocationProvider {

    val param: String
    fun DRI.toLocation(): String
}

interface ExternalLocationProviderFactory {

    fun getExternalLocationProvider(param: String): ExternalLocationProvider?
}

class ExternalLocationProviderFactoryWithCache(val ext: ExternalLocationProviderFactory) : ExternalLocationProviderFactory {

    private val locationProviders = ConcurrentHashMap<String, CacheWrapper>()

    override fun getExternalLocationProvider(param: String): ExternalLocationProvider? =
        locationProviders.getOrPut(param) { CacheWrapper(ext.getExternalLocationProvider(param)) }.provider
}

private class CacheWrapper(val provider: ExternalLocationProvider?)