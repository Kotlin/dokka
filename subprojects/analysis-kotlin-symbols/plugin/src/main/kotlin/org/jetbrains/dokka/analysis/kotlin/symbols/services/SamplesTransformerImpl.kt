package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SamplesKotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.getPsiFilesFromPaths
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.getSourceFilePaths
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.doc.Sample
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile


internal const val KOTLIN_PLAYGROUND_SCRIPT = "<script src=\"`https://unpkg.com/kotlin-playground@1`\"></script>"

internal abstract class SamplesTransformerImpl(val context: DokkaContext) : PageTransformer {

    abstract fun processBody(psiElement: PsiElement): String
    abstract fun processImports(psiElement: PsiElement): String

    final override fun invoke(input: RootPageNode): RootPageNode {

        val analysis = SamplesKotlinAnalysis(
            sourceSets = context.configuration.sourceSets,
            context = context,
            projectKotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
        )
        return input.transformContentPagesTree { page ->
            val samples = (page as? WithDocumentables)?.documentables?.flatMap {
                it.documentation.entries.flatMap { entry ->
                    entry.value.children.filterIsInstance<Sample>().map { entry.key to it }
                }
            }

            samples?.fold(page as ContentPage) { acc, (sampleSourceSet, sample) ->
                acc.modified(
                    content = acc.content.addSample(page, sampleSourceSet, sample.name, analysis),
                    embeddedResources = acc.embeddedResources + KOTLIN_PLAYGROUND_SCRIPT
                )
            } ?: page
        }
    }


    private fun ContentNode.addSample(
        contentPage: ContentPage,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        fqName: String,
        kotlinAnalysis: KotlinAnalysis
    ): ContentNode {
        val analysisContext = kotlinAnalysis[sourceSet]
        val psiElement = analyze(analysisContext.mainModule) {
            val lastDotIndex = fqName.lastIndexOf('.')

            val functionName = if (lastDotIndex == -1) fqName else fqName.substring(lastDotIndex + 1, fqName.length)
            val packageName = if (lastDotIndex == -1) "" else fqName.substring(0, lastDotIndex)
            getTopLevelCallableSymbols(FqName(packageName), Name.identifier(functionName)).firstOrNull()?.psi
        }
            ?: return this.also { context.logger.warn("Cannot find PsiElement corresponding to $fqName") }
        val imports =
            processImports(psiElement)
        val body = processBody(psiElement)
        val node =
            contentCode(contentPage.content.sourceSets, contentPage.dri, createSampleBody(imports, body), "kotlin")

        return dfs(fqName, node)
    }

    protected open fun createSampleBody(imports: String, body: String) =
        """ |$imports
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
