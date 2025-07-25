/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.templating.AddToSourcesetDependencies
import org.jetbrains.dokka.base.templating.toJsonString
import org.jetbrains.dokka.pages.RendererSpecificResourcePage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.transformers.pages.PageTransformer

public open class NavigationPageInstaller(
    public val context: DokkaContext
) : NavigationDataProvider(context), PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode =
        input.modified(
            children = input.children + NavigationPage(
                root = navigableChildren(input),
                moduleName = context.configuration.moduleName,
                context = context
            )
        )
}

public class CustomResourceInstaller(
    public val dokkaContext: DokkaContext
) : PageTransformer {
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

public class ScriptsInstaller(private val dokkaContext: DokkaContext) : PageTransformer {

    /*
    * By default, scripts are loaded as `async` scripts.
     * Scripts ending with `_deferred.js` are loaded with `defer`.
     * Scripts ending with `_blocking.js` are loaded as regular i.e. blocking scripts
     */
    private val scriptsPages = listOf(
        // it's important for this script to be blocking because other scripts depend on it
        "scripts/safe-local-storage_blocking.js",
        "scripts/navigation-loader.js",
        "scripts/platform-content-handler.js",
        "scripts/main.js",
        "scripts/prism.js",
        "ui-kit/ui-kit.min.js"
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

public class StylesInstaller(private val dokkaContext: DokkaContext) : PageTransformer {
    private val stylesPages = listOf(
        "styles/style.css",
        "styles/main.css",
        "styles/prism.css",
        "styles/logo-styles.css",
        "ui-kit/ui-kit.min.css"
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

public object AssetsInstaller : PageTransformer {
    private val imagesPages = listOf(
        // images
        "images/abstract-class-kotlin.svg",
        "images/abstract-class.svg",
        "images/annotation-kotlin.svg",
        "images/annotation.svg",
        "images/arrow-down.svg",
        "images/burger.svg",
        "images/check.svg",
        "images/checkbox-off.svg",
        "images/checkbox-on.svg",
        "images/class-kotlin.svg",
        "images/class.svg",
        "images/cross.svg",
        "images/enum-kotlin.svg",
        "images/enum.svg",
        "images/exception-class.svg",
        "images/field-value.svg",
        "images/field-variable.svg",
        "images/filter.svg",
        "images/function.svg",
        "images/go-to-top-icon.svg",
        "images/homepage.svg",
        "images/interface-kotlin.svg",
        "images/interface.svg",
        "images/logo-icon.svg",
        "images/object.svg",
        "images/placeholder.svg",
        "images/theme-toggle.svg",
        "images/typealias-kotlin.svg",
        "images/copy-icon.svg",
        "images/success-icon.svg",
        "images/anchor-copy-icon.svg",

        // fonts
        "ui-kit/fonts/inter-latin-400-italic.woff",
        "ui-kit/fonts/inter-latin-400-italic.woff2",
        "ui-kit/fonts/inter-latin-400-normal.woff",
        "ui-kit/fonts/inter-latin-400-normal.woff2",
        "ui-kit/fonts/inter-latin-600-normal.woff",
        "ui-kit/fonts/inter-latin-600-normal.woff2",
        "ui-kit/fonts/jetbrains-mono-latin-400-normal.woff",
        "ui-kit/fonts/jetbrains-mono-latin-400-normal.woff2",
        "ui-kit/fonts/jetbrains-mono-latin-600-normal.woff",
        "ui-kit/fonts/jetbrains-mono-latin-600-normal.woff2"
    )

    override fun invoke(input: RootPageNode): RootPageNode = input.modified(
        children = input.children + imagesPages.toRenderSpecificResourcePage()
    )
}

private fun List<String>.toRenderSpecificResourcePage(): List<RendererSpecificResourcePage> =
    map { RendererSpecificResourcePage(it, emptyList(), RenderingStrategy.Copy("/dokka/$it")) }

public class SourcesetDependencyAppender(
    public val context: DokkaContext
) : PageTransformer {
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
