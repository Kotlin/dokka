package  org.jetbrains.dokka.mathjax


import org.jetbrains.dokka.model.doc.CustomWrapperTag
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.pages.PageNodeTransformer

class MathjaxPlugin : DokkaPlugin() {
    val transformer by extending {
        CoreExtensions.pageTransformer with MathjaxTransformer
    }
}

private const val ANNOTATION = "usesMathJax"
private const val LIB_PATH = "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.6/MathJax.js?config=TeX-AMS_SVG&latest"

object MathjaxTransformer : PageNodeTransformer {
    override fun invoke(input: RootPageNode) = input.transformContentPagesTree {
        it.modified(
            embeddedResources = it.embeddedResources + if (it.isNeedingMathjax) listOf(LIB_PATH) else emptyList()
        )
    }

    private val ContentPage.isNeedingMathjax
        get() = documentable?.platformInfo
            ?.flatMap { it.documentationNode.children }
            .orEmpty()
            .any { (it as? CustomWrapperTag)?.name == ANNOTATION }
}