package javadoc.pages

import org.jetbrains.dokka.pages.RendererSpecificResourcePage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.transformers.pages.PageTransformer

val preprocessors = listOf(ResourcesInstaller, AllClassesPageInstaller)

object AllClassesPageInstaller : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode {
        val classes = (input as JavadocModulePageNode).children.filterIsInstance<JavadocPackagePageNode>().flatMap {
            it.children
        }

        return input.modified(children = input.children + AllClassesPage(classes))
    }
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