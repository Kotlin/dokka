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

    private val locationProviders = ConcurrentHashMap<String, ExternalLocationProvider>()

    override fun getExternalLocationProvider(param: String): ExternalLocationProvider? =
        locationProviders.getOrPut(param) { ext.getExternalLocationProvider(param) }
}