package javadoc.pages

import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.transformers.pages.PageTransformer

val preprocessors = listOf(ResourcesInstaller)

object LinkCacher : PageTransformer {
    private val _linkMap: MutableMap<String, DCI> = mutableMapOf()
    val linkMap: Map<String, DCI>
        get() = _linkMap.toMap()

    private fun mapLinks(node: PageNode) {
        if (node is ContentNode) _linkMap[node.name] = node.dci
        node.children.forEach(::mapLinks)
    }

    override fun invoke(input: RootPageNode): RootPageNode = input.also(::mapLinks)
}

//object RootInstaller : PageTransformer {
//    override fun invoke(input: RootPageNode) =
//        JavadocModulePageNode("", input.children, input)
//}

object ResourcesInstaller : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode = input.modified(
        children = input.children +
                RendererSpecificResourcePage(
                    "resourcePack",
                    emptyList(),
                    RenderingStrategy.Copy("static_res")
                )
    )
}