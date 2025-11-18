/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinplaygroundsamples

import org.jetbrains.dokka.pages.RendererSpecificResourcePage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.transformers.pages.PageTransformer

public class KotlinPlaygroundSamplesScriptsInstaller(private val dokkaContext: DokkaContext) : PageTransformer {

    private val kotlinPlaygroundServer =
        configuration<KotlinPlaygroundSamplesPlugin, KotlinPlaygroundSamplesConfiguration>(dokkaContext)?.kotlinPlaygroundServer

    private val scriptsPages = listOf(
        "scripts/kotlin-playground-samples.js"
    )

    override fun invoke(input: RootPageNode): RootPageNode =
        input.let { root ->
            if (dokkaContext.configuration.delayTemplateSubstitution) root
            else {
                if (kotlinPlaygroundServer == null) {
                    root.modified(children = input.children + scriptsPages.toRenderSpecificResourcePage())
                } else {
                    val modifiedScript = modifyScript()

                    root.modified(children = input.children + scriptsPages.map {
                        RendererSpecificResourcePage(
                            it,
                            emptyList(),
                            RenderingStrategy.Write(modifiedScript)
                        )
                    })
                }
            }
        }.transformContentPagesTree {
            it.modified(
                embeddedResources = it.embeddedResources + scriptsPages
            )
        }

    private fun modifyScript(): String {
        val scriptContent = javaClass.getResource("/dokka/${scriptsPages.first()}")
            ?.readText()
            ?: throw IllegalStateException("Script /dokka/${scriptsPages.first()} not found in resources")

        return scriptContent.replace(
            "const kotlinPlaygroundServer = null",
            "const kotlinPlaygroundServer = \"$kotlinPlaygroundServer\""
        )
    }
}

public class KotlinPlaygroundSamplesStylesInstaller(private val dokkaContext: DokkaContext) : PageTransformer {
    private val stylesPages = listOf(
        "styles/kotlin-playground-samples.css"
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

private fun List<String>.toRenderSpecificResourcePage(): List<RendererSpecificResourcePage> =
    map { RendererSpecificResourcePage(it, emptyList(), RenderingStrategy.Copy("/dokka/$it")) }
