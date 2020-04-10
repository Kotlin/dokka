package org.jetbrains.dokka.base.transformers.pages.samples

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.DokkaGenerator
import org.jetbrains.dokka.EnvironmentAndFacade
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.AnalysisEnvironment
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.base.renderers.platforms
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.Sample
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.idea.kdoc.resolveKDocSampleLink
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File

abstract class SamplesTransformer(val context: DokkaContext) : PageTransformer {

    abstract fun processBody(psiElement: PsiElement): String
    abstract fun processImports(psiElement: PsiElement): String

    final override fun invoke(input: RootPageNode): RootPageNode {

        val analysis = setUpAnalysis(context)

        return input.transformContentPagesTree { page ->
            page.documentable?.documentation?.allEntries?.fold(page) { acc, entry ->
                entry.second.children.filterIsInstance<Sample>().fold(acc) { acc, sample ->
                    acc.modified(content = acc.content.addSample(page, entry.first, sample.name, analysis))
                }
            } ?: page
        }
    }

    private fun setUpAnalysis(context: DokkaContext) = context.configuration.passesConfigurations.map {
        it.platformData to AnalysisEnvironment(DokkaGenerator.DokkaMessageCollector(context.logger), it.analysisPlatform).run {
            if (analysisPlatform == Platform.jvm) {
                addClasspath(PathUtil.getJdkClassesRootsFromCurrentJre())
            }
            it.classpath.forEach { addClasspath(File(it)) }

            addSources(it.samples.map { it })

            loadLanguageVersionSettings(it.languageVersion, it.apiVersion)

            val environment = createCoreEnvironment()
            val (facade, _) = createResolutionFacade(environment)
            EnvironmentAndFacade(environment, facade)
        }
    }.toMap()

    private fun ContentNode.addSample(contentPage: ContentPage, platform: PlatformData?, fqName: String, analysis: Map<PlatformData, EnvironmentAndFacade>): ContentNode {
        val facade = if(platform == null) {
            analysis.entries.find { it.key.platformType.name == "common" }?.value
        } else {
            analysis[platform]
        }?.facade ?: return this.also { context.logger.warn("Cannot resolve facade for platform ${platform?.name ?: "expect"}") }
        val psiElement = fqNameToPsiElement(facade, fqName) ?:
            return this.also { context.logger.warn("Cannot find PsiElement corresponding to $fqName") }
        val imports = processImports(psiElement) // TODO: Process somehow imports. Maybe just attach them at the top of each body
        val body = processBody(psiElement)
        val node = contentCode(contentPage.platforms(), contentPage.dri, body, "kotlin")

        return bfs(fqName, node)
    }

    private fun ContentNode.bfs(fqName: String, node: ContentCode): ContentNode {
        return when(this) {
            is ContentHeader -> copy(children.map { it.bfs(fqName, node) })
            is ContentCode -> copy(children.map { it.bfs(fqName, node) })
            is ContentDRILink -> copy(children.map { it.bfs(fqName, node) })
            is ContentResolvedLink -> copy(children.map { it.bfs(fqName, node) })
            is ContentEmbeddedResource -> copy(children.map { it.bfs(fqName, node) })
            is ContentTable -> copy(children.map { it.bfs(fqName, node) as ContentGroup })
            is ContentList -> copy(children.map { it.bfs(fqName, node) })
            is ContentGroup -> copy(children.map { it.bfs(fqName, node) })
            is PlatformHintedContent -> copy(inner.bfs(fqName, node))
            is ContentText -> if (text == fqName) node else this
            else -> this
        }
    }

    private fun fqNameToPsiElement(resolutionFacade: DokkaResolutionFacade, functionName: String): PsiElement? {
        val packageName = functionName.takeWhile { it != '.' }
        val descriptor = resolutionFacade.resolveSession.getPackageFragment(FqName(packageName)) ?:
            return null.also { context.logger.warn("Cannot find descriptor for package $packageName") }
        val symbol = resolveKDocSampleLink(BindingContext.EMPTY, resolutionFacade, descriptor, functionName.split(".")).firstOrNull() ?:
            return null.also { context.logger.warn("Unresolved function $functionName in @sample") }
        return DescriptorToSourceUtils.descriptorToDeclaration(symbol)
    }

    private fun contentCode(platforms: List<PlatformData>, dri: Set<DRI>, content: String, language: String) =
        ContentCode(
            children = listOf(
                ContentText(
                    text = content,
                    dci = DCI(dri, ContentKind.BriefComment),
                    platforms = platforms.toSet(),
                    style = emptySet(),
                    extra = PropertyContainer.empty()
                )
            ),
            language = language,
            extra = PropertyContainer.empty(),
            dci = DCI(dri, ContentKind.Source),
            platforms = platforms.toSet(),
            style = emptySet()
        )
}