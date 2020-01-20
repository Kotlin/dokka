package  org.jetbrains.dokka.kotlinAsJava


import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.single
import org.jetbrains.dokka.transformers.documentation.DocumentationToPageTranslator

class KotlinAsJavaPlugin : DokkaPlugin() {
    val kotlinAsJavaTranslator by extending { CoreExtensions.documentationToPageTranslator with KotlinAsJavaTranslator }
}

object KotlinAsJavaTranslator : DocumentationToPageTranslator {
    override fun invoke(module: Module, context: DokkaContext): ModulePageNode =
        KotlinAsJavaPageBuilder { node, kind, operation ->
            KotlinAsJavaPageContentBuilder.group(
                node.dri,
                node.platformData,
                kind,
                context.single(CoreExtensions.commentsToContentConverter),
                context.logger,
                operation
            )
        }.pageForModule(module)

}