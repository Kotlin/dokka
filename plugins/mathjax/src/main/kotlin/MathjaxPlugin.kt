package  org.jetbrains.dokka.mathjax

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.PageNodeTransformer

class MathjaxPlugin : DokkaPlugin() {
    val transformer by extending {
        CoreExtensions.pageTransformer with MathjaxTransformer
    }
}

private const val ANNOTATION = "@usesMathJax"
private const val LIB_PATH = "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.6/MathJax.js?config=TeX-AMS_SVG&latest"

object MathjaxTransformer : PageNodeTransformer {
    override fun invoke(input: ModulePageNode, dokkaContext: DokkaContext) = input.modified(
        children = input.children.map { transform(it) }
    )

    private fun transform(input: PageNode): PageNode = input.modified(
        embeddedResources = input.embeddedResources + if (input.isNeedingMathjax) listOf(LIB_PATH) else emptyList(),
        children = input.children.map { transform(it) }
    )


    private val PageNode.isNeedingMathjax
        get() = documentationNode?.platformInfo
            ?.flatMap { it.docTag?.children?.toList().orEmpty() }
            .orEmpty()
            .any { it.text == ANNOTATION }
}