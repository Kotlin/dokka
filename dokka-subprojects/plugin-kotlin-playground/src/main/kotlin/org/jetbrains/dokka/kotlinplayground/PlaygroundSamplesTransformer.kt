/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinplayground

import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.transformers.pages.PageTransformer

internal const val DEFAULT_KOTLIN_PLAYGROUND_SCRIPT = "https://unpkg.com/kotlin-playground@1/dist/playground.min.js"

/**
 * Transforms sample code blocks to make them runnable with Kotlin Playground.
 * 
 * This transformer finds code blocks that contain samples (identified by ContentKind.Sample)
 * and modifies them to be runnable by adding the RunnableSample style and embedding
 * the Kotlin Playground script.
 */
internal class PlaygroundSamplesTransformer(private val context: DokkaContext) : PageTransformer {

    private val configuration: KotlinPlaygroundConfiguration by lazy {
        context.plugin<KotlinPlaygroundPlugin>().query { configuration }
            .firstOrNull() ?: KotlinPlaygroundConfiguration()
    }

    override fun invoke(input: RootPageNode): RootPageNode {
        val playgroundScript = configuration.playgroundScript ?: DEFAULT_KOTLIN_PLAYGROUND_SCRIPT
        
        return input.transformContentPagesTree { page ->
            // Check if this page has any sample content
            val hasSamples = page.content.hasSampleContent()
            
            if (hasSamples) {
                // Transform the content to add runnable sample styles
                val newContent = page.content.transformSamples()
                
                page.modified(
                    content = newContent,
                    embeddedResources = page.embeddedResources + playgroundScript
                )
            } else {
                page
            }
        }
    }

    /**
     * Checks if the content node contains any sample content.
     */
    private fun ContentNode.hasSampleContent(): Boolean {
        return when (this) {
            is ContentCodeBlock -> dci.kind == ContentKind.Sample
            is ContentComposite -> children.any { it.hasSampleContent() }
            else -> false
        }
    }

    /**
     * Transforms sample content to make it runnable.
     */
    private fun ContentNode.transformSamples(): ContentNode {
        return when (this) {
            is ContentCodeBlock -> {
                if (dci.kind == ContentKind.Sample) {
                    // Add the RunnableSample style to make the code block interactive
                    copy(style = style + ContentStyle.RunnableSample)
                } else {
                    this
                }
            }
            is ContentHeader -> copy(children = children.map { it.transformSamples() })
            is ContentDivergentGroup -> @Suppress("UNCHECKED_CAST") copy(
                children = children.map { it.transformSamples() } as List<ContentDivergentInstance>
            )
            is ContentDivergentInstance -> copy(
                before = before?.transformSamples(),
                divergent = divergent.transformSamples(),
                after = after?.transformSamples()
            )
            is ContentCodeInline -> copy(children = children.map { it.transformSamples() })
            is ContentDRILink -> copy(children = children.map { it.transformSamples() })
            is ContentResolvedLink -> copy(children = children.map { it.transformSamples() })
            is ContentEmbeddedResource -> copy(children = children.map { it.transformSamples() })
            is ContentTable -> copy(children = children.map { it.transformSamples() as ContentGroup })
            is ContentList -> copy(children = children.map { it.transformSamples() })
            is ContentGroup -> copy(children = children.map { it.transformSamples() })
            is PlatformHintedContent -> copy(inner = inner.transformSamples())
            else -> this
        }
    }
}