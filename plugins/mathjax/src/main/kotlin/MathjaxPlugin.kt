package  org.jetbrains.dokka.mathjax


import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.pages.PageTransformer

class MathjaxPlugin : DokkaPlugin() {
    val transformer by extending {
        CoreExtensions.pageTransformer with MathjaxTransformer
    }
}

private const val ANNOTATION = "usesMathJax"
private const val LIB_PATH = "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.6/MathJax.js?config=TeX-AMS_SVG&latest"

object MathjaxTransformer : PageTransformer {
    override fun invoke(input: RootPageNode) = input.transformContentPagesTree {
        it.modified(
            embeddedResources = it.embeddedResources + if (it.isNeedingMathjax) listOf(LIB_PATH) else emptyList()
        )
    }

    private val ContentPage.isNeedingMathjax
        get() = documentable?.documentation?.values
            ?.flatMap { it.children }
            .orEmpty()
            .any { (it as? CustomTagWrapper)?.name == ANNOTATION }
}