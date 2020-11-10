package org.jetbrains.dokka.base.renderers.html

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DEnumEntry
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.transformers.pages.PageTransformer

object NavigationPageInstaller : PageTransformer {
    private val mapper = jacksonObjectMapper()

    fun SearchRecord.Companion.from(node: NavigationNode, location: String): SearchRecord =
        SearchRecord(name = node.name, location = location)

    override fun invoke(input: RootPageNode): RootPageNode {
        val nodes = input.children.filterIsInstance<ContentPage>().single()
            .let(NavigationPageInstaller::visit)

        val page = RendererSpecificResourcePage(
            name = "scripts/navigation-pane.json",
            children = emptyList(),
            strategy = RenderingStrategy.LocationResolvableWrite { resolver ->
                mapper.writeValueAsString(
                    nodes.withDescendants().map { SearchRecord.from(it, resolver(it.dri, it.sourceSets)) })
            })

        return input.modified(
            children = input.children + page + NavigationPage(nodes)
        )
    }

    private fun visit(page: ContentPage): NavigationNode =
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
        val withEmbeddedResources = input.transformContentPagesTree { it.modified(embeddedResources = it.embeddedResources + customResourcesPaths) }
        val (currentResources, otherPages) = withEmbeddedResources.children.partition { it is RendererSpecificResourcePage }
        return input.modified(children = otherPages + currentResources.filterNot { it.name in customResourcesPaths } + customAssets + customStylesheets)
    }
}

object ScriptsInstaller : PageTransformer {
    private val scriptsPages = listOf(
        "scripts/clipboard.js",
        "scripts/navigation-loader.js",
        "scripts/platform-content-handler.js",
        "scripts/main.js",
    )

    override fun invoke(input: RootPageNode): RootPageNode {
        return input.modified(
            children = input.children + scriptsPages.toRenderSpecificResourcePage()
        ).transformContentPagesTree {
            it.modified(
                embeddedResources = it.embeddedResources + scriptsPages
            )
        }
    }
}

object StylesInstaller : PageTransformer {
    private val stylesPages = listOf(
        "styles/style.css",
        "styles/logo-styles.css",
        "styles/jetbrains-mono.css",
        "styles/main.css"
    )

    override fun invoke(input: RootPageNode): RootPageNode =
        input.modified(
            children = input.children + stylesPages.toRenderSpecificResourcePage()
        ).transformContentPagesTree {
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
        val dependenciesMap = context.configuration.sourceSets.map {
            it.sourceSetID to it.dependentSourceSets
        }.toMap()

        fun createDependenciesJson(): String = "sourceset_dependencies = '{${
            dependenciesMap.entries.joinToString(", ") {
                "\"${it.key}\": [${
                    it.value.joinToString(",") {
                        "\"$it\""
                    }
                }]"
            }
        }}'"

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


