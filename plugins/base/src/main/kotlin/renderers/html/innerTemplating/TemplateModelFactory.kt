package org.jetbrains.dokka.base.renderers.html.innerTemplating

import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.pages.PageNode

interface TemplateModelFactory {
    fun buildModel(
        page: PageNode,
        resources: List<String>,
        locationProvider: LocationProvider,
        content: String
    ): TemplateMap

    fun buildSharedModel(): TemplateMap
}
