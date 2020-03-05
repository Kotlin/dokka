package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.links.DRI


interface ExternalLocationProvider {

    val param: String
    fun DRI.toLocation(): String
}

interface ExternalLocationProviderFactory {

    fun getExternalLocationProvider(param: String): ExternalLocationProvider?
}

class ExternalLocationProviderFactoryWithCache(val ext: ExternalLocationProviderFactory) : ExternalLocationProviderFactory {

    private val locationProviders: MutableList<ExternalLocationProvider> = mutableListOf()

    override fun getExternalLocationProvider(param: String): ExternalLocationProvider? =
        locationProviders.find { it.param == param } ?: ext.getExternalLocationProvider(param)?.also { locationProviders.add(it) }
}