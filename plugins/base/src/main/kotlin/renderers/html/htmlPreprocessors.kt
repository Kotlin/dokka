package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.templating.AddToSourcesetDependencies
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.base.transformers.documentables.isException


abstract class NavigationDataProvider {
    open fun navigableChildren(input: RootPageNode): NavigationNode = input.withDescendants()
        .first { it is ModulePage || it is MultimoduleRootPage }.let { visit(it as ContentPage) }

    open fun visit(page: ContentPage): NavigationNode = NavigationNode(
        name = page.displayableName(),
        dri = page.dri.first(),
        sourceSets = page.sourceSets(),
        icon = chooseNavigationIcon(page),
        children = page.navigableChildren()
    )

    /**
     * Parenthesis is applied in 1 case:
     *  - page only contains functions (therefore documentable from this page is [DFunction])
     */
    private fun ContentPage.displayableName(): String =
        if (this is WithDocumentables && documentables.all { it is DFunction }) {
            "$name()"
        } else {
            name
        }

    private fun chooseNavigationIcon(contentPage: ContentPage): NavigationNodeIcon? {
        return if (contentPage is WithDocumentables) {
            when(val documentable = contentPage.documentables.firstOrNull()) {
                is DClass -> when {
                    documentable.isException -> NavigationNodeIcon.EXCEPTION
                    documentable.isAbstract() -> NavigationNodeIcon.ABSTRACT_CLASS
                    else -> NavigationNodeIcon.CLASS
                }
                is DFunction -> NavigationNodeIcon.FUNCTION
                is DProperty -> {
                    val isVar = documentable.extra[IsVar] != null
                    if (isVar) NavigationNodeIcon.VAR else NavigationNodeIcon.VAL
                }
                is DInterface -> NavigationNodeIcon.INTERFACE
                is DEnum -> NavigationNodeIcon.ENUM_CLASS
                is DAnnotation -> NavigationNodeIcon.ANNOTATION_CLASS
                is DObject -> NavigationNodeIcon.OBJECT
                else -> null
            }
        } else {
            null
        }
    }

    private fun DClass.isAbstract(): Boolean {
        return modifier.values.all { it is KotlinModifier.Abstract || it is JavaModifier.Abstract }
    }

    private fun ContentPage.navigableChildren(): List<NavigationNode> {
        return if (this !is ClasslikePageNode) {
            children
                .filterIsInstance<ContentPage>()
                .map { visit(it) }
                .sortedBy { it.name.toLowerCase() }
        } else {
            emptyList()
        }
    }
}

open class NavigationPageInstaller(val context: DokkaContext) : NavigationDataProvider(), PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode =
        input.modified(
            children = input.children + NavigationPage(
                root = navigableChildren(input),
                moduleName = context.configuration.moduleName,
                context = context
            )
        )
}

class CustomResourceInstaller(val dokkaContext: DokkaContext) : PageTransformer {
    private val configuration = configuration<DokkaBase, DokkaBaseConfiguration>(dokkaContext)

    private val customAssets = configuration?.customAssets?.map {
        RendererSpecificResourcePage("images/${it.name}", emptyList(), RenderingStrategy.Copy(it.absolutePath))
    }.orEmpty()

    private val customStylesheets = configuration?.customStyleSheets?.map {
        RendererSpecificResourcePage("styles/${it.name}", emptyList(), RenderingStrategy.Copy(it.absolutePath))
    }.orEmpty()

    override fun invoke(input: RootPageNode): RootPageNode {
        val customResourcesPaths = (customAssets + customStylesheets).map { it.name }.toSet()
        val withEmbeddedResources =
            input.transformContentPagesTree { it.modified(embeddedResources = it.embeddedResources + customResourcesPaths) }
        if(dokkaContext.configuration.delayTemplateSubstitution)
            return withEmbeddedResources
        val (currentResources, otherPages) = withEmbeddedResources.children.partition { it is RendererSpecificResourcePage }
        return input.modified(children = otherPages + currentResources.filterNot { it.name in customResourcesPaths } + customAssets + customStylesheets)
    }
}

class ScriptsInstaller(private val dokkaContext: DokkaContext) : PageTransformer {
    private val scriptsPages = listOf(
        "scripts/clipboard.js",
        "scripts/navigation-loader.js",
        "scripts/platform-content-handler.js",
        "scripts/main.js",
        "scripts/prism.js"
    )

    override fun invoke(input: RootPageNode): RootPageNode =
        input.let { root ->
            if (dokkaContext.configuration.delayTemplateSubstitution) root
            else root.modified(children = input.children + scriptsPages.toRenderSpecificResourcePage())
        }.transformContentPagesTree {
            it.modified(
                embeddedResources = it.embeddedResources + scriptsPages
            )
        }
}

class StylesInstaller(private val dokkaContext: DokkaContext) : PageTransformer {
    private val stylesPages = listOf(
        "styles/style.css",
        "styles/jetbrains-mono.css",
        "styles/main.css",
        "styles/prism.css",
        "styles/logo-styles.css"
    )

    override fun invoke(input: RootPageNode): RootPageNode =
        input.let { root ->
            if (dokkaContext.configuration.delayTemplateSubstitution) root
            else root.modified(children = input.children + stylesPages.toRenderSpecificResourcePage())
        }.transformContentPagesTree {
            it.modified(
                embeddedResources = it.embeddedResources + stylesPages
            )
        }
}

object AssetsInstaller : PageTransformer {
    private val imagesPages = listOf(
        "images/arrow_down.svg",
        "images/logo-icon.svg",
        "images/go-to-top-icon.svg",
        "images/footer-go-to-link.svg",
        "images/anchor-copy-button.svg",
        "images/copy-icon.svg",
        "images/copy-successful-icon.svg",
        "images/theme-toggle.svg",
        "images/nav-icons/abstract-class.svg",
        "images/nav-icons/annotation.svg",
        "images/nav-icons/class.svg",
        "images/nav-icons/enum.svg",
        "images/nav-icons/exception-class.svg",
        "images/nav-icons/field-value.svg",
        "images/nav-icons/field-variable.svg",
        "images/nav-icons/function.svg",
        "images/nav-icons/interface.svg",
        "images/nav-icons/object.svg",
    )

    override fun invoke(input: RootPageNode) = input.modified(
        children = input.children + imagesPages.toRenderSpecificResourcePage()
    )
}

private fun List<String>.toRenderSpecificResourcePage(): List<RendererSpecificResourcePage> =
    map { RendererSpecificResourcePage(it, emptyList(), RenderingStrategy.Copy("/dokka/$it")) }

class SourcesetDependencyAppender(val context: DokkaContext) : PageTransformer {
    private val name = "scripts/sourceset_dependencies.js"
    override fun invoke(input: RootPageNode): RootPageNode {
        val dependenciesMap = context.configuration.sourceSets.associate {
            it.sourceSetID to it.dependentSourceSets
        }

        fun createDependenciesJson(): String =
            dependenciesMap.map { (key, values) -> key.toString() to values.map { it.toString() } }.toMap()
                .let { content ->
                    if (context.configuration.delayTemplateSubstitution) {
                        toJsonString(AddToSourcesetDependencies(context.configuration.moduleName, content))
                    } else {
                        "sourceset_dependencies='${toJsonString(content)}'"
                    }
                }

        val deps = RendererSpecificResourcePage(
            name = name,
            children = emptyList(),
            strategy = RenderingStrategy.Write(createDependenciesJson())
        )

        return input.modified(
            children = input.children + deps
        ).transformContentPagesTree { it.modified(embeddedResources = it.embeddedResources + name) }
    }
}
