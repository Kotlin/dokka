package org.jetbrains.dokka.html.renderers.innerTemplating

import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.pages.PageNode

interface TemplateModelFactory {
    fun buildModel(
        page: PageNode,
        resources: List<String>,
        locationProvider: LocationProvider,
        shouldRenderSourceSetBubbles: Boolean,
        content: String
    ): TemplateMap

    fun buildSharedModel(): TemplateMap
}