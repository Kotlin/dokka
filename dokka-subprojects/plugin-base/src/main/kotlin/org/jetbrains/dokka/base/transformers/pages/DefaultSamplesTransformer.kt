/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.pages

import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.doc.Sample
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironmentCreator
import org.jetbrains.dokka.analysis.kotlin.sample.SampleSnippet

internal const val KOTLIN_PLAYGROUND_SCRIPT = "https://unpkg.com/kotlin-playground@1/dist/playground.min.js"

/**
 * It works ONLY with a content model from the base plugin.
 */
internal class DefaultSamplesTransformer(val context: DokkaContext) : PageTransformer {

    private val sampleAnalysisEnvironment: SampleAnalysisEnvironmentCreator =
        context.plugin<KotlinAnalysisPlugin>().querySingle { sampleAnalysisEnvironmentCreator }

    override fun invoke(input: RootPageNode): RootPageNode {
        return sampleAnalysisEnvironment.use {
            input.transformContentPagesTree { page ->
                val samples = (page as? WithDocumentables)?.documentables?.flatMap {
                    it.documentation.entries.flatMap { entry ->
                        entry.value.children.filterIsInstance<Sample>().map { entry.key to it }
                    }
                } ?: return@transformContentPagesTree page

                val newContent = samples.fold(page.content) { acc, (sampleSourceSet, sample) ->
                    resolveSample(sampleSourceSet, sample.name)
                        ?.let {
                            acc.addSample(page, sample.name, it)
                        } ?: acc
                }

                page.modified(
                    content = newContent,
                    embeddedResources = page.embeddedResources + KOTLIN_PLAYGROUND_SCRIPT
                )
            }
        }
    }
    private fun ContentNode.addSample(
        contentPage: ContentPage,
        fqLink: String,
        sample: SampleSnippet,
    ): ContentNode {
        val node = contentCode(contentPage.content.sourceSets, contentPage.dri, createSampleBody(sample.imports, sample.body), "kotlin")
        return dfs(fqLink, node)
    }

    /**
     * If both [imports] and [body] are present, it should return
     *
     * ```kotlin
     * import com.example.One
     * import com.example.Two
     *
     * fun main() {
     *    //sampleStart
     *    println("Sample function body")
     *    println("Another line")
     *    //sampleEnd
     * }
     * ```
     *
     * If [imports] are empty, it should return:
     *
     * ```kotlin
     * fun main() {
     *    //sampleStart
     *    println("Sample function body")
     *    println("Another line")
     *    //sampleEnd
     * }
     * ```
     *
     * Notice the presence/absence of the new line before the body.
     */
    private fun createSampleBody(imports: List<String>, body: String) =
        // takeIf {} is needed so that joinToString's postfix is not added for empty lists,
        // and trimMargin() then removes the first empty line
        """ |${imports.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n", postfix = "\n") { "import $it" } ?: "" }
            |fun main() { 
            |   //sampleStart 
            |   $body 
            |   //sampleEnd
            |}""".trimMargin()

    private fun ContentNode.dfs(fqName: String, node: ContentCodeBlock): ContentNode {
        return when (this) {
            is ContentHeader -> copy(children.map { it.dfs(fqName, node) })
            is ContentDivergentGroup -> @Suppress("UNCHECKED_CAST") copy(children.map {
                it.dfs(fqName, node)
            } as List<ContentDivergentInstance>)
            is ContentDivergentInstance -> copy(
                before.let { it?.dfs(fqName, node) },
                divergent.dfs(fqName, node),
                after.let { it?.dfs(fqName, node) })
            is ContentCodeBlock -> copy(children.map { it.dfs(fqName, node) })
            is ContentCodeInline -> copy(children.map { it.dfs(fqName, node) })
            is ContentDRILink -> copy(children.map { it.dfs(fqName, node) })
            is ContentResolvedLink -> copy(children.map { it.dfs(fqName, node) })
            is ContentEmbeddedResource -> copy(children.map { it.dfs(fqName, node) })
            is ContentTable -> copy(children = children.map { it.dfs(fqName, node) as ContentGroup })
            is ContentList -> copy(children.map { it.dfs(fqName, node) })
            is ContentGroup -> copy(children.map { it.dfs(fqName, node) })
            is PlatformHintedContent -> copy(inner.dfs(fqName, node))
            is ContentText -> if (text == fqName) node else this
            is ContentBreakLine -> this
            else -> this.also { context.logger.error("Could not recognize $this ContentNode in SamplesTransformer") }
        }
    }

    private fun contentCode(
        sourceSets: Set<DisplaySourceSet>,
        dri: Set<DRI>,
        content: String,
        language: String,
        styles: Set<Style> = emptySet(),
        extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
    ) =
        ContentCodeBlock(
            children = listOf(
                ContentText(
                    text = content,
                    dci = DCI(dri, ContentKind.Sample),
                    sourceSets = sourceSets,
                    style = emptySet(),
                    extra = PropertyContainer.empty()
                )
            ),
            language = language,
            dci = DCI(dri, ContentKind.Sample),
            sourceSets = sourceSets,
            style = styles + ContentStyle.RunnableSample + TextStyle.Monospace,
            extra = extra
        )
}
