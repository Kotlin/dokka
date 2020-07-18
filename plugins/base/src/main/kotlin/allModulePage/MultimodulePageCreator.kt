package org.jetbrains.dokka.base.allModulePage

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.DokkaException
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.MultimoduleLocationProvider.Companion.MULTIMODULE_PACKAGE_PLACEHOLDER
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.base.parsers.MarkdownParser
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageCreator
import org.jetbrains.dokka.utilities.DokkaLogger
import java.io.File

class MultimodulePageCreator(
    val context: DokkaContext
) : PageCreator {
    private val logger: DokkaLogger = context.logger

    override fun invoke(): RootPageNode {
        val parser = MarkdownParser(logger = logger)
        val modules = context.configuration.modules
        modules.forEach(::throwOnMissingModuleDocFile)

        val commentsConverter = context.plugin(DokkaBase::class)?.querySingle { commentsToContentConverter }
        val signatureProvider = context.plugin(DokkaBase::class)?.querySingle { signatureProvider }
        if (commentsConverter == null || signatureProvider == null)
            throw IllegalStateException("Both comments converter and signature provider must not be null")

        val sourceSetData = emptySet<DokkaSourceSet>()
        val builder = PageContentBuilder(commentsConverter, signatureProvider, context.logger)
        val contentNode = builder.contentFor(
            dri = DRI(MULTIMODULE_PACKAGE_PLACEHOLDER),
            kind = ContentKind.Cover,
            sourceSets = sourceSetData
        ) {
            header(2, "All modules:")
            table(styles = setOf(MultimoduleTable)) {
                modules.mapNotNull { module ->
                    val paragraph = module.docFile.readText().let { parser.parse(it).firstParagraph() }
                    paragraph?.let {
                        val dri = DRI(packageName = MULTIMODULE_PACKAGE_PLACEHOLDER, classNames = module.name)
                        val dci = DCI(setOf(dri), ContentKind.Main)
                        val header =
                            ContentHeader(listOf(linkNode(module.name, dri)), 2, dci, emptySet(), emptySet())
                        val content = ContentGroup(
                            DocTagToContentConverter.buildContent(it, dci, emptySet()),
                            dci,
                            emptySet(),
                            emptySet()
                        )
                        ContentGroup(listOf(header, content), dci, emptySet(), emptySet())
                    }
                }
            }
        }
        return MultimoduleRootPageNode(
            "Modules",
            setOf(DRI(packageName = MULTIMODULE_PACKAGE_PLACEHOLDER, classNames = "allModules")),
            contentNode
        )
    }

    private fun throwOnMissingModuleDocFile(module: DokkaConfiguration.DokkaModuleDescription) {
        if (!module.docFile.exists() || !module.docFile.isFile) {
            throw DokkaException(
                "Missing documentation file for module ${module.name}: ${module.docFile.absolutePath}"
            )
        }
    }

    private fun DocumentationNode.firstParagraph() =
        this.children.flatMap { it.root.children }.filterIsInstance<P>().firstOrNull()
}
