package org.jetbrains.dokka.base.renderers.html

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.renderers.pageId
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.templating.AddToSourcesetDependencies
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.transformers.pages.PageTransformer

abstract class NavigationDataProvider {
    open fun navigableChildren(input: RootPageNode, moduleName: String, locationResolver: DriResolver): NavigationNode =
        input.children.filterIsInstance<ContentPage>().single()
            .let { NavigationDataVisitor(locationResolver, moduleName).visit(it) }

    private class NavigationDataVisitor(val locationResolver: DriResolver, val moduleName: String) {
        fun visit(page: ContentPage): NavigationNode =
            NavigationNode(
                id = page.pageId.formatWithModuleName(),
                name = page.displayableName,
                location = locationResolver(page.dri.first(), page.sourceSets()),
                children = page.navigableChildren()
            )

        private fun ContentPage.navigableChildren(): List<NavigationNode> =
            when (documentable) {
                is DModule -> children.filterIsInstance<ContentPage>().map { visit(it) }
                    .sortedBy { it.name.toLowerCase() }
                is DPackage -> listOfNotNull(
                    NavigationNode(
                        "$pageId/types".formatWithModuleName(),
                        "Types",
                        locationResolver(dri.first(), sourceSets()),
                        childrenOfType<ClasslikePageNode>().map { visit(it) }.sortedBy { it.name.toLowerCase() }
                    ).takeIf { it.children.isNotEmpty() },
                    NavigationNode(
                        "$pageId/properties".formatWithModuleName(),
                        "Properties",
                        locationResolver(dri.first(), sourceSets()),
                        children.filterIsInstance<ContentPage>().filter { it.documentable is DProperty }
                            .map { visit(it) }
                            .sortedBy { it.name.toLowerCase() })
                        .takeIf { it.children.isNotEmpty() },
                    NavigationNode(
                        "$pageId/functions".formatWithModuleName(),
                        "Functions",
                        locationResolver(dri.first(), sourceSets()),
                        children.filterIsInstance<ContentPage>().filter { it.documentable is DFunction }
                            .map { visit(it) }
                            .sortedBy { it.name.toLowerCase() })
                        .takeIf { it.children.isNotEmpty() },
                ).map { it.copy(location = "${it.location}?active-tab=${it.name}") }
                is DClasslike -> children.filterIsInstance<ClasslikePageNode>()
                    .map { visit(it) }.sortedBy { it.name.toLowerCase() }
                else -> emptyList()
            }

        private val ContentPage.displayableName: String
            get() = if (documentable is DFunction) {
                "$name()"
            } else {
                name
            }

        private fun String.formatWithModuleName(): String = "$moduleName::$this"
    }
}

open class NavigationSearchInstaller(val context: DokkaContext) : NavigationDataProvider(), PageTransformer {
    private val mapper = jacksonObjectMapper()

    open fun createSearchRecordFromNode(node: NavigationNode, location: String): SearchRecord =
        SearchRecord(name = node.name, location = location)

    override fun invoke(input: RootPageNode): RootPageNode {
//        val page = RendererSpecificResourcePage(
//            name = "scripts/navigation-pane.js",
//            children = emptyList(),
//            strategy = RenderingStrategy.DriLocationResolvableWrite { resolver ->
//                val content = navigableChildren(input, context.configuration.moduleName).withDescendants().map {
//                    createSearchRecordFromNode(it, resolveLocation(resolver, it.dri, it.sourceSets).orEmpty())
//                }
//                if (context.configuration.delayTemplateSubstitution) {
//                    mapper.writeValueAsString(AddToSearch(context.configuration.moduleName, content.toList()))
//                } else {
//                    "var navigationPane = " + mapper.writeValueAsString(content)
//                }
//            })

//        return input.modified(children = input.children + page)
        return input
    }

    private fun resolveLocation(locationResolver: DriResolver, dri: DRI, sourceSets: Set<DisplaySourceSet>): String? =
        locationResolver(dri, sourceSets).also { location ->
            if (location.isNullOrBlank()) context.logger.warn("Cannot resolve path for $dri and sourceSets: ${sourceSets.joinToString { it.name }}")
        }
}

open class NavigationPageInstaller(val context: DokkaContext) : NavigationDataProvider(), PageTransformer {
    private val mapper = jacksonObjectMapper()

    override fun invoke(input: RootPageNode): RootPageNode {
        val page = RendererSpecificResourcePage(
            name = "scripts/navigation.js",
            children = emptyList(),
            strategy = RenderingStrategy.DriLocationResolvableWrite { resolver ->
                val content = navigableChildren(input, context.configuration.moduleName, resolver)
                "var navigation = " + mapper.writeValueAsString(listOf(content))
            })

        return input
            .modified(children = input.children + page)
            .transformContentPagesTree { it.modified(embeddedResources = listOf(page.name) + it.embeddedResources) }
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
        val withEmbeddedResources =
            input.transformContentPagesTree { it.modified(embeddedResources = it.embeddedResources + customResourcesPaths) }
        val (currentResources, otherPages) = withEmbeddedResources.children.partition { it is RendererSpecificResourcePage }
        return input.modified(children = otherPages + currentResources.filterNot { it.name in customResourcesPaths } + customAssets + customStylesheets)
    }
}

class ScriptsInstaller(private val dokkaContext: DokkaContext) : PageTransformer {
    private val scriptsPages = listOf(
        "scripts/clipboard.js",
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
        val dependenciesMap = context.configuration.sourceSets.map {
            it.sourceSetID to it.dependentSourceSets
        }.toMap()

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