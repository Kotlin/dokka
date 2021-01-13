package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentation.Classifier.Module
import org.jetbrains.dokka.base.parsers.moduleAndPackage.ModuleAndPackageDocumentationParsingContext
import org.jetbrains.dokka.base.parsers.moduleAndPackage.parseModuleAndPackageDocumentation
import org.jetbrains.dokka.base.parsers.moduleAndPackage.parseModuleAndPackageDocumentationFragments
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.base.templating.InsertTemplateExtra
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageCreator
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.versioning.ReplaceVersionsCommand
import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin

class MultimodulePageCreator(
    private val context: DokkaContext,
) : PageCreator<AllModulesPageGeneration.DefaultAllModulesContext> {
    private val logger: DokkaLogger = context.logger

    private val commentsConverter by lazy { context.plugin<DokkaBase>().querySingle { commentsToContentConverter } }
    private val signatureProvider by lazy { context.plugin<DokkaBase>().querySingle { signatureProvider } }

    override fun invoke(creationContext: AllModulesPageGeneration.DefaultAllModulesContext): RootPageNode {
        val modules = context.configuration.modules
        val sourceSetData = emptySet<DokkaSourceSet>()
        val builder = PageContentBuilder(commentsConverter, signatureProvider, context.logger)
        val contentNode = builder.contentFor(
            dri = DRI(MULTIMODULE_PACKAGE_PLACEHOLDER),
            kind = ContentKind.Cover,
            sourceSets = sourceSetData
        ) {
            /* The line below checks if there is a provided configuration for versioning.
             If not, we are skipping the template for inserting versions navigation */
            configuration<VersioningPlugin, VersioningConfiguration>(context)?.let {
                group(extra = PropertyContainer.withAll(InsertTemplateExtra(ReplaceVersionsCommand))) { }
            }
            header(2, "All modules:")
            table(styles = setOf(MultimoduleTable)) {
                modules.filter { it.name in creationContext.nonEmptyModules }.sortedByDescending { it.name }
                    .forEach { module ->
                        val displayedModuleDocumentation = getDisplayedModuleDocumentation(module)
                        val dri = DRI(packageName = MULTIMODULE_PACKAGE_PLACEHOLDER, classNames = module.name)
                        val dci = DCI(setOf(dri), ContentKind.Comment)
                        val extraWithAnchor = PropertyContainer.withAll(SymbolAnchorHint(module.name, ContentKind.Main))
                        row(setOf(dri), emptySet(), styles = emptySet(), extra = extraWithAnchor) {
                            +linkNode(module.name, dri, DCI(setOf(dri), ContentKind.Main), extra = extraWithAnchor)
                            +ContentGroup(
                                children =
                                if (displayedModuleDocumentation != null)
                                    DocTagToContentConverter().buildContent(
                                        displayedModuleDocumentation,
                                        dci,
                                        emptySet()
                                    )
                                else emptyList(),
                                dci = dci,
                                sourceSets = emptySet(),
                                style = emptySet()
                            )
                        }
                    }
            }
        }
        return MultimoduleRootPageNode(
            setOf(MULTIMODULE_ROOT_DRI),
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

    companion object {
        const val MULTIMODULE_PACKAGE_PLACEHOLDER = ".ext"
        val MULTIMODULE_ROOT_DRI = DRI(packageName = MULTIMODULE_PACKAGE_PLACEHOLDER, classNames = "allModules")
    }
}
