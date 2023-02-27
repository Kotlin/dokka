package  org.jetbrains.dokka.mathjax


import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.transformers.pages.tags.CustomTagContentProvider
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder.DocumentableContentBuilder
import org.jetbrains.dokka.model.doc.CustomTagWrapper
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.pages.WithDocumentables
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.transformers.pages.PageTransformer

class MathjaxPlugin : DokkaPlugin() {
    val transformer by extending {
        CoreExtensions.pageTransformer with MathjaxTransformer
    }

    val mathjaxTagContentProvider by extending {
        plugin<DokkaBase>().customTagContentProvider with MathjaxTagContentProvider order {
            before(plugin<DokkaBase>().sinceKotlinTagContentProvider)
        }
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}

private const val ANNOTATION = "usesMathJax"
internal const val LIB_PATH = "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.6/MathJax.js?config=TeX-AMS_SVG&latest"

object MathjaxTransformer : PageTransformer {
    override fun invoke(input: RootPageNode) = input.transformContentPagesTree {
        it.modified(
            embeddedResources = it.embeddedResources + if (it.isNeedingMathjax) listOf(LIB_PATH) else emptyList()
        )
    }

    private val ContentPage.isNeedingMathjax
        get() = (this as WithDocumentables).documentables.any { it.documentation.values
            .flatMap { it.children }
            .any { (it as? CustomTagWrapper)?.name == ANNOTATION } }
}
object MathjaxTagContentProvider : CustomTagContentProvider {

    override fun isApplicable(customTag: CustomTagWrapper) = customTag.name == ANNOTATION

    override fun DocumentableContentBuilder.contentForDescription(
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        customTag: CustomTagWrapper
    ) {
        comment(customTag.root, sourceSets = setOf(sourceSet))
    }
}