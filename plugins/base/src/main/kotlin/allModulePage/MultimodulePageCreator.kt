package org.jetbrains.dokka.base.allModulePage

import org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.parsers.MarkdownParser
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentation.Classifier.Module
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentationParsingContext
import org.jetbrains.dokka.base.parsers.moduleAndPackage.parseModuleAndPackageDocumentation
import org.jetbrains.dokka.base.parsers.moduleAndPackage.parseModuleAndPackageDocumentationFragments
import org.jetbrains.dokka.base.resolvers.local.MultimoduleLocationProvider.Companion.MULTIMODULE_PACKAGE_PLACEHOLDER
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageCreator
import org.jetbrains.dokka.utilities.DokkaLogger

class MultimodulePageCreator(
    private val context: DokkaContext,
) : PageCreator {
    private val logger: DokkaLogger = context.logger

    override fun invoke(): RootPageNode {
        val parser = MarkdownParser(logger = logger)
        val modules = context.configuration.modules

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
                modules.map { module ->
                    val displayedModuleDocumentation = getDisplayedModuleDocumentation(module)
                    val dri = DRI(packageName = MULTIMODULE_PACKAGE_PLACEHOLDER, classNames = module.name)
                    val dci = DCI(setOf(dri), ContentKind.Comment)
                    val header =
                        ContentHeader(listOf(linkNode(module.name, dri)), 2, dci, emptySet(), emptySet())
                    val content = ContentGroup(
                        children =
                        if (displayedModuleDocumentation != null)
                            DocTagToContentConverter.buildContent(displayedModuleDocumentation, dci, emptySet())
                        else emptyList(),
                        dci = dci,
                        sourceSets = emptySet(),
                        style = emptySet()
                    )
                    ContentGroup(listOf(header, content), dci, emptySet(), emptySet())
                }
            }
        }
        return MultimoduleRootPageNode(
            "Modules",
            setOf(DRI(packageName = MULTIMODULE_PACKAGE_PLACEHOLDER, classNames = "allModules")),
            contentNode
        )
    }

    private fun getDisplayedModuleDocumentation(module: DokkaModuleDescription): P? {
        val parsingContext = ModuleAndPackageDocumentationParsingContext(logger)

        val documentationFragment = module.includes
            .flatMap { include -> parseModuleAndPackageDocumentationFragments(include) }
            .firstOrNull { fragment -> fragment.classifier == Module && fragment.name == module.name }
            ?: return null

        val moduleDocumentation = parseModuleAndPackageDocumentation(parsingContext, documentationFragment)
        return moduleDocumentation.documentation.firstParagraph()
    }

    private fun DocumentationNode.firstParagraph(): P? =
        this.children
            .map { it.root }
            .mapNotNull { it.firstParagraph() }
            .firstOrNull()

    /**
     * @return The very first, most inner paragraph. If any [P] is wrapped inside another [P], the inner one
     * is preferred.
     */
    private fun DocTag.firstParagraph(): P? {
        val firstChildParagraph = children.mapNotNull { it.firstParagraph() }.firstOrNull()
        return if (firstChildParagraph == null && this is P) this
        else firstChildParagraph
    }
}
