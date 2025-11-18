/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinPlaygroundSamples

import org.jetbrains.dokka.pages.*
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
            if (it.containsRunnableSample()) {
                it.modified(
                    embeddedResources = it.embeddedResources + scriptsPages
                )
            } else {
                it
            }
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
            if (it.containsRunnableSample()) {
                it.modified(
                    embeddedResources = it.embeddedResources + stylesPages
                )
            } else {
                it
            }
        }
}

private fun List<String>.toRenderSpecificResourcePage(): List<RendererSpecificResourcePage> =
    map { RendererSpecificResourcePage(it, emptyList(), RenderingStrategy.Copy("/dokka/$it")) }

private fun ContentPage.containsRunnableSample(): Boolean {
    fun ContentNode.hasRunnableSample(): Boolean {
        val bool = when (this) {
            is ContentCodeBlock -> style.contains(ContentStyle.RunnableSample)
            is ContentGroup -> children.any { it.hasRunnableSample() }
            is ContentHeader -> children.any { it.hasRunnableSample() }
            is ContentList -> children.any { it.hasRunnableSample() }
            is ContentTable -> children.any { it.hasRunnableSample() }
            is ContentDivergentGroup -> children.any { it.hasRunnableSample() }
            is ContentDivergentInstance -> before?.hasRunnableSample() == true ||
                    divergent.hasRunnableSample() ||
                    after?.hasRunnableSample() == true

            is PlatformHintedContent -> inner.hasRunnableSample()
            is ContentDRILink -> children.any { it.hasRunnableSample() }
            is ContentResolvedLink -> children.any { it.hasRunnableSample() }
            is ContentEmbeddedResource -> children.any { it.hasRunnableSample() }
            is ContentCodeInline -> children.any { it.hasRunnableSample() }
            else -> false
        }
        return bool
    }

    return content.hasRunnableSample()
}
