package org.jetbrains.dokka.newFrontend.transformers

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.dokka.newFrontend.pages.ModulePageNode
import org.jetbrains.dokka.newFrontend.pages.PackagePageNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.newFrontend.renderer.MarkdownRenderer

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
    val name: String,
    val description: String
)

class NewFrontendToViewTransformer : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode =
        when(input){
            is ModulePageNode -> input.toView()
            else -> input
        }

    fun ModulePageNode.toView(): ModulePageNodeView = ModulePageNodeView(
        name = name,
        description = MarkdownRenderer.render(description),
        content = content.packages.map {
            ModulePagePackageElementView(it.name, it.dri.toString())
        },
        children = children.map { it.toView() }
    )

    fun PackagePageNode.toView(): PackagePageNodeView = PackagePageNodeView(
        name = name,
        description = MarkdownRenderer.render(content.description)
    )
}