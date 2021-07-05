package org.jetbrains.dokka.base.renderers.html

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.templating.AddToSearch
import org.jetbrains.dokka.base.templating.AddToSourcesetDependencies
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.transformers.pages.PageTransformer

abstract class NavigationDataProvider {
    open fun navigableChildren(input: RootPageNode): NavigationNode = input.withDescendants()
            .first { it is ModulePage || it is MultimoduleRootPage }.let { visit(it as ContentPage) }

    open fun visit(page: ContentPage): NavigationNode =
        NavigationNode(
            name = page.displayableName,
            dri = page.dri.first(),
            sourceSets = page.sourceSets(),
            children = page.navigableChildren()
        )

    private fun ContentPage.navigableChildren(): List<NavigationNode> =
        when {
            this !is ClasslikePageNode ->
                children.filterIsInstance<ContentPage>().map { visit(it) }
            documentable is DEnum ->
                children.filter { it is ContentPage && it.documentable is DEnumEntry }.map { visit(it as ContentPage) }
            else -> emptyList()
        }.sortedBy { it.name.toLowerCase() }

    private val ContentPage.displayableName: String
        get() = if (documentable is DFunction) {
            "$name()"
        } else {
            name
        }
}

open class NavigationSearchInstaller(val context: DokkaContext) : NavigationDataProvider(), PageTransformer {
    private val mapper = jacksonObjectMapper()

    open fun createSearchRecordFromNode(node: NavigationNode, location: String): SearchRecord =
        SearchRecord(name = node.name, location = location)

    override fun invoke(input: RootPageNode): RootPageNode {
        val page = RendererSpecificResourcePage(
            name = "scripts/navigation-pane.json",
            children = emptyList(),
            strategy = RenderingStrategy.DriLocationResolvableWrite { resolver ->
                val content = navigableChildren(input).withDescendants().map {
                    createSearchRecordFromNode(it, resolveLocation(resolver, it.dri, it.sourceSets).orEmpty())
                }
                if (context.configuration.delayTemplateSubstitution) {
                    mapper.writeValueAsString(AddToSearch(context.configuration.moduleName, content.toList()))
                } else {
                    mapper.writeValueAsString(content)
                }
            })

        return input.modified(children = input.children + page)
    }

    private fun resolveLocation(locationResolver: DriResolver, dri: DRI, sourceSets: Set<DisplaySourceSet>): String? =
        locationResolver(dri, sourceSets).also { location ->
            if (location.isNullOrBlank()) context.logger.warn("Cannot resolve path for $dri and sourceSets: ${sourceSets.joinToString { it.name }}")
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
        "styles/logo-styles.css",
        "styles/jetbrains-mono.css",
        "styles/main.css"
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
        "images/docs_logo.svg",
        "images/logo-icon.svg",
        "images/go-to-top-icon.svg",
        "images/footer-go-to-link.svg",
        "images/anchor-copy-button.svg",
        "images/copy-icon.svg",
        "images/copy-successful-icon.svg",
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
