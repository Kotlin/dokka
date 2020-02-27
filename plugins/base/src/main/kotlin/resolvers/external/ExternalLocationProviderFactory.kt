package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.links.DRI


interface ExternalLocationProvider {

    val param: String
    fun DRI.toLocation(): String
}

interface ExternalLocationProviderFactory {
    fun getExternalLocationProvider(param: String): ExternalLocationProvider?
}

abstract class ExternalLocationProviderFactoryWithCache : ExternalLocationProviderFactory {

    private val locationProviders: MutableList<ExternalLocationProvider> = mutableListOf()

    override fun getExternalLocationProvider(param: String): ExternalLocationProvider? =
        locationProviders.find { it.param == param } ?: createExternalLocationProvider(param)?.also { locationProviders.add(it) }

    protected abstract fun createExternalLocationProvider(param: String): ExternalLocationProvider?
}