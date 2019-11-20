package org.jetbrains.dokka

import org.jetbrains.dokka.Model.transformers.DocumentationNodeTransformer
import org.jetbrains.dokka.plugability.ExtensionPoint
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.resolvers.LocationProvider
import org.jetbrains.dokka.transformers.PageNodeTransformer

object CoreExtensions {
    val nodeTransformer = ExtensionPoint<DocumentationNodeTransformer>(this::class.qualifiedName!!, "nodeTransformer")
    val pageTransformer = ExtensionPoint<PageNodeTransformer>(this::class.qualifiedName!!, "pageTransformer")
    val renderer = ExtensionPoint<Renderer>(this::class.qualifiedName!!, "renderer")
    val locationProvider = ExtensionPoint<LocationProvider>(this::class.qualifiedName!!, "locationProvider")
}