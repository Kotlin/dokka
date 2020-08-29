package org.jetbrains.dokka.newFrontend.transformers

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.dokka.newFrontend.pages.ModulePageNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.transformers.pages.PageTransformer

@Serializable
data class ModulePageNodeView(
    override val name: String,
    val content: List<ModulePagePackageElementView>,
    @Transient override val children: List<PageNode> = emptyList()
): RootPageNode() {
    override fun modified(name: String, children: List<PageNode>): RootPageNode = copy(
        name = name,
        content = content,
        children = children
    )
}

@Serializable
data class ModulePagePackageElementView(
    val name: String,
    val location: String
)

class NewFrontendToViewTransformer : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode =
        when(input){
            is ModulePageNode -> input.toView()
            else -> input
        }

    fun ModulePageNode.toView(): ModulePageNodeView = ModulePageNodeView(
        name = name,
        content = content.packages.map {
            ModulePagePackageElementView(it.name, it.dri.toString())
        },
        children = children)
}