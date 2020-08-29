package org.jetbrains.dokka.newFrontend.transformers

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.base.resolvers.local.resolveOrThrow
import org.jetbrains.dokka.newFrontend.pages.ModulePageNode
import org.jetbrains.dokka.newFrontend.pages.PackagePageNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.newFrontend.renderer.MarkdownRenderer
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle

@Serializable
data class ModulePageNodeView(
    override val name: String,
    val description: String,
    val content: List<ModulePagePackageElementView>,
    @Transient override val children: List<PackagePageNodeView> = emptyList()
): RootPageNode() {
    override fun modified(name: String, children: List<PageNode>): RootPageNode = this
}

@Serializable
data class ModulePagePackageElementView(
    val name: String,
    val location: String
)

@Serializable
data class PackagePageNodeView(
    override val name: String,
    val description: String,
    @Transient override val children: List<PackagePageNodeView> = emptyList()
): PageNode {
    override fun modified(name: String, children: List<PageNode>): PageNode = this
}

class NewFrontendToViewTransformer(val context: DokkaContext) : PageTransformer {
    private lateinit var locationProvider: LocationProvider

    override fun invoke(input: RootPageNode): RootPageNode {
        locationProvider = context.plugin<DokkaBase>().querySingle { locationProviderFactory }.getLocationProvider(input)
        return when(input){
            is ModulePageNode -> input.toView()
            else -> input
        }
    }

    fun ModulePageNode.toView(): ModulePageNodeView = ModulePageNodeView(
        name = name,
        description = MarkdownRenderer.render(description),
        content = content.packages.map {
            ModulePagePackageElementView(it.name, locationProvider.resolveOrThrow(it.dri, it.sourceSets))
        },
        children = children.map { it.toView() }
    )

    fun PackagePageNode.toView(): PackagePageNodeView = PackagePageNodeView(
        name = name,
        description = MarkdownRenderer.render(content.description)
    )
}