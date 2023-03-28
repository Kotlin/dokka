@file:Suppress("unused")

package org.jetbrains.dokka.base

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.analysis.KotlinAnalysis
import org.jetbrains.dokka.analysis.ProjectKotlinAnalysis
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.analysis.java.*
import org.jetbrains.dokka.analysis.java.doctag.DocTagParserContext
import org.jetbrains.dokka.analysis.java.doctag.InheritDocTagContentProvider
import org.jetbrains.dokka.analysis.java.parsers.*
import org.jetbrains.dokka.analysis.java.parsers.doctag.InheritDocTagResolver
import org.jetbrains.dokka.analysis.java.parsers.doctag.PsiDocTagParser
import org.jetbrains.dokka.base.generation.SingleModuleGeneration
import org.jetbrains.dokka.base.parsers.MarkdownParser
import org.jetbrains.dokka.base.renderers.*
import org.jetbrains.dokka.base.renderers.html.*
import org.jetbrains.dokka.base.renderers.html.command.consumers.PathToRootConsumer
import org.jetbrains.dokka.base.renderers.html.command.consumers.ReplaceVersionsConsumer
import org.jetbrains.dokka.base.renderers.html.command.consumers.ResolveLinkConsumer
import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.javadoc.JavadocExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.base.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.templating.ImmediateHtmlCommandConsumer
import org.jetbrains.dokka.base.transformers.documentables.*
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.base.transformers.pages.merger.*
import org.jetbrains.dokka.base.transformers.pages.samples.DefaultSamplesTransformer
import org.jetbrains.dokka.base.transformers.pages.sourcelinks.SourceLinksTransformer
import org.jetbrains.dokka.base.transformers.pages.tags.CustomTagContentProvider
import org.jetbrains.dokka.base.transformers.pages.tags.SinceKotlinTagContentProvider
import org.jetbrains.dokka.base.translators.descriptors.DefaultDescriptorToDocumentableTranslator
import org.jetbrains.dokka.base.translators.descriptors.DefaultExternalDocumentablesProvider
import org.jetbrains.dokka.base.translators.descriptors.ExternalClasslikesTranslator
import org.jetbrains.dokka.base.translators.descriptors.ExternalDocumentablesProvider
import org.jetbrains.dokka.base.translators.documentables.DefaultDocumentableToPageTranslator
import org.jetbrains.dokka.base.utils.NoopIntellijLoggerFactory
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import java.io.File
import java.util.*

class DokkaBase : DokkaPlugin() {

    val preMergeDocumentableTransformer by extensionPoint<PreMergeDocumentableTransformer>()
    val pageMergerStrategy by extensionPoint<PageMergerStrategy>()
    val commentsToContentConverter by extensionPoint<CommentsToContentConverter>()
    val customTagContentProvider by extensionPoint<CustomTagContentProvider>()
    val signatureProvider by extensionPoint<SignatureProvider>()
    val locationProviderFactory by extensionPoint<LocationProviderFactory>()
    val externalLocationProviderFactory by extensionPoint<ExternalLocationProviderFactory>()
    val outputWriter by extensionPoint<OutputWriter>()
    val htmlPreprocessors by extensionPoint<PageTransformer>()
    val kotlinAnalysis by extensionPoint<KotlinAnalysis>()

    @Deprecated("It is not used anymore")
    val tabSortingStrategy by extensionPoint<TabSortingStrategy>()
    val immediateHtmlCommandConsumer by extensionPoint<ImmediateHtmlCommandConsumer>()
    val externalDocumentablesProvider by extensionPoint<ExternalDocumentablesProvider>()
    val externalClasslikesTranslator by extensionPoint<ExternalClasslikesTranslator>()

    val singleGeneration by extending {
        CoreExtensions.generation providing ::SingleModuleGeneration
    }

    val descriptorToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing ::DefaultDescriptorToDocumentableTranslator
    }

    val psiToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing { context ->
            DefaultPsiToDocumentableTranslator(
                javaAnalysisHelper = DokkaAnalysisJavaHelper(),
            )
        }
    }

    val documentableMerger by extending {
        CoreExtensions.documentableMerger providing ::DefaultDocumentableMerger
    }

    val deprecatedDocumentableFilter by extending {
        preMergeDocumentableTransformer providing ::DeprecatedDocumentableFilterTransformer
    }

    val suppressedDocumentableFilter by extending {
        preMergeDocumentableTransformer providing ::SuppressedByConfigurationDocumentableFilterTransformer
    }

    val suppressedBySuppressTagDocumentableFilter by extending {
        preMergeDocumentableTransformer providing ::SuppressTagDocumentableFilter
    }

    val documentableVisibilityFilter by extending {
        preMergeDocumentableTransformer providing ::DocumentableVisibilityFilterTransformer
    }

    val obviousFunctionsVisbilityFilter by extending {
        preMergeDocumentableTransformer providing ::ObviousFunctionsDocumentableFilterTransformer
    }

    val inheritedEntriesVisbilityFilter by extending {
        preMergeDocumentableTransformer providing ::InheritedEntriesDocumentableFilterTransformer
    }

    val kotlinArrayDocumentableReplacer by extending {
        preMergeDocumentableTransformer providing ::KotlinArrayDocumentableReplacerTransformer
    }

    val emptyPackagesFilter by extending {
        preMergeDocumentableTransformer providing ::EmptyPackagesFilterTransformer order {
            after(
                deprecatedDocumentableFilter,
                suppressedDocumentableFilter,
                documentableVisibilityFilter,
                suppressedBySuppressTagDocumentableFilter,
                obviousFunctionsVisbilityFilter,
                inheritedEntriesVisbilityFilter,
            )
        }
    }

    val emptyModulesFilter by extending {
        preMergeDocumentableTransformer with EmptyModulesFilterTransformer() order {
            after(emptyPackagesFilter)
        }
    }

    val modulesAndPackagesDocumentation by extending {
        preMergeDocumentableTransformer providing ::ModuleAndPackageDocumentationTransformer
    }

    val actualTypealiasAdder by extending {
        CoreExtensions.documentableTransformer with ActualTypealiasAdder()
    }

    val kotlinSignatureProvider by extending {
        signatureProvider providing ::KotlinSignatureProvider
    }

    val sinceKotlinTransformer by extending {
        CoreExtensions.documentableTransformer providing ::SinceKotlinTransformer applyIf { SinceKotlinTransformer.shouldDisplaySinceKotlin() } order {
            before(extensionsExtractor)
        }
    }

    val inheritorsExtractor by extending {
        CoreExtensions.documentableTransformer with InheritorsExtractorTransformer()
    }

    val undocumentedCodeReporter by extending {
        CoreExtensions.documentableTransformer with ReportUndocumentedTransformer()
    }

    val extensionsExtractor by extending {
        CoreExtensions.documentableTransformer with ExtensionExtractorTransformer()
    }

    val documentableToPageTranslator by extending {
        CoreExtensions.documentableToPageTranslator providing ::DefaultDocumentableToPageTranslator
    }

    val docTagToContentConverter by extending {
        commentsToContentConverter with DocTagToContentConverter()
    }

    val sinceKotlinTagContentProvider by extending {
        customTagContentProvider with SinceKotlinTagContentProvider applyIf { SinceKotlinTransformer.shouldDisplaySinceKotlin() }
    }

    val pageMerger by extending {
        CoreExtensions.pageTransformer providing ::PageMerger
    }

    val sourceSetMerger by extending {
        CoreExtensions.pageTransformer providing ::SourceSetMergingPageTransformer
    }

    val fallbackMerger by extending {
        pageMergerStrategy providing { ctx -> FallbackPageMergerStrategy(ctx.logger) }
    }

    val sameMethodNameMerger by extending {
        pageMergerStrategy providing { ctx -> SameMethodNamePageMergerStrategy(ctx.logger) } order {
            before(fallbackMerger)
        }
    }

    val htmlRenderer by extending {
        CoreExtensions.renderer providing ::HtmlRenderer
    }

    val defaultKotlinAnalysis by extending {
        kotlinAnalysis providing { ctx ->
            ProjectKotlinAnalysis(
                sourceSets = ctx.configuration.sourceSets,
                logger = ctx.logger
            )
        }
    }

    val locationProvider by extending {
        locationProviderFactory providing ::DokkaLocationProviderFactory
    }

    val javadocLocationProvider by extending {
        externalLocationProviderFactory providing ::JavadocExternalLocationProviderFactory
    }

    val dokkaLocationProvider by extending {
        externalLocationProviderFactory providing ::DefaultExternalLocationProviderFactory
    }

    val fileWriter by extending {
        outputWriter providing ::FileWriter
    }

    val rootCreator by extending {
        htmlPreprocessors with RootCreator applyIf { !delayTemplateSubstitution }
    }

    val defaultSamplesTransformer by extending {
        CoreExtensions.pageTransformer providing ::DefaultSamplesTransformer order {
            before(pageMerger)
        }
    }

    val sourceLinksTransformer by extending {
        htmlPreprocessors providing ::SourceLinksTransformer order { after(rootCreator) }
    }

    val navigationPageInstaller by extending {
        htmlPreprocessors providing ::NavigationPageInstaller order { after(rootCreator) }
    }

    val scriptsInstaller by extending {
        htmlPreprocessors providing ::ScriptsInstaller order { after(rootCreator) }
    }

    val stylesInstaller by extending {
        htmlPreprocessors providing ::StylesInstaller order { after(rootCreator) }
    }

    val assetsInstaller by extending {
        htmlPreprocessors with AssetsInstaller order { after(rootCreator) } applyIf { !delayTemplateSubstitution }
    }

    val customResourceInstaller by extending {
        htmlPreprocessors providing { ctx -> CustomResourceInstaller(ctx) } order {
            after(stylesInstaller)
            after(scriptsInstaller)
            after(assetsInstaller)
        }
    }

    val packageListCreator by extending {
        htmlPreprocessors providing {
            PackageListCreator(it, RecognizedLinkFormat.DokkaHtml)
        } order { after(rootCreator) }
    }

    val sourcesetDependencyAppender by extending {
        htmlPreprocessors providing ::SourcesetDependencyAppender order { after(rootCreator) }
    }

    val resolveLinkConsumer by extending {
        immediateHtmlCommandConsumer with ResolveLinkConsumer
    }
    val replaceVersionConsumer by extending {
        immediateHtmlCommandConsumer providing ::ReplaceVersionsConsumer
    }
    val pathToRootConsumer by extending {
        immediateHtmlCommandConsumer with PathToRootConsumer
    }
    val baseSearchbarDataInstaller by extending {
        htmlPreprocessors providing ::SearchbarDataInstaller order { after(sourceLinksTransformer) }
    }

    val defaultExternalDocumentablesProvider by extending {
        externalDocumentablesProvider providing ::DefaultExternalDocumentablesProvider
    }

    val defaultExternalClasslikesTranslator by extending {
        externalClasslikesTranslator providing ::DefaultDescriptorToDocumentableTranslator
    }

    internal val disposeKotlinAnalysisPostAction by extending {
        CoreExtensions.postActions with PostAction { this@DokkaBase.querySingle { kotlinAnalysis }.close() }
    }

    private companion object {
        init {
            // Suppress messages emitted by the IntelliJ logger since
            // there's not much the end user can do about it
            com.intellij.openapi.diagnostic.Logger.setFactory(NoopIntellijLoggerFactory())
        }
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}

class DokkaAnalysisJavaHelper : JavaAnalysisHelper {

    override fun extractSourceRoots(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): List<File> {
        val kotlinAnalysis = context.plugin<DokkaBase>().querySingle { kotlinAnalysis }
        val (environment, facade) = kotlinAnalysis[sourceSet]
        return environment.configuration.get(CLIConfigurationKeys.CONTENT_ROOTS)
            ?.filterIsInstance<JavaSourceRoot>()
            ?.mapNotNull { it.file.takeIf { isFileInSourceRoots(it, sourceSet) } }
            ?: listOf()
    }

    private fun isFileInSourceRoots(file: File, sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean =
        sourceSet.sourceRoots.any { root -> file.startsWith(root) }

    override fun extractProject(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): Project {
        val kotlinAnalysis = context.plugin<DokkaBase>().querySingle { kotlinAnalysis }
        return kotlinAnalysis[sourceSet].facade.project
    }

    override fun createPsiParser(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): DokkaPsiParser {
        val kotlinAnalysis = context.plugin<DokkaBase>().querySingle { kotlinAnalysis }

        val docCommentFactory = DocCommentFactory(
            docCommentCreators = listOf(
                JavaDocCommentCreator(),
                KotlinDocCommentCreator()
            )
        )
        val docCommentFinder = DocCommentFinder(
            logger = context.logger,
            docCommentFactory = docCommentFactory
        )
        val kotlinDocCommentParser = KotlinDocCommentParser(
            resolutionFacade = kotlinAnalysis[sourceSet].facade,
            logger = context.logger
        )
        val psiDocTagParser = PsiDocTagParser(
            inheritDocTagResolver = InheritDocTagResolver(
                docCommentFactory = docCommentFactory,
                docCommentFinder = docCommentFinder,
                contentProviders = listOf(KotlinInheritDocTagContentProvider(kotlinDocCommentParser))
            )
        )
        return DokkaPsiParser(
            sourceSetData = sourceSet,
            project = extractProject(sourceSet, context),
            logger = context.logger,
            javadocParser = JavadocParser(
                docCommentParsers = listOf(
                    JavaPsiDocCommentParser(
                        psiDocTagParser = psiDocTagParser
                    ),
                    kotlinDocCommentParser
                ),
                docCommentFinder = docCommentFinder
            ),
            javaPsiDocCommentParser = JavaPsiDocCommentParser(
                psiDocTagParser = psiDocTagParser
            ),
            isLightAnnotation = { it is KtLightAbstractAnnotation },
            isLightAnnotationAttribute = { it is KtLightAbstractAnnotation }
        )
    }
}

internal class KotlinInheritDocTagContentProvider(
    val parser: KotlinDocCommentParser
) : InheritDocTagContentProvider {

    override fun canConvert(content: DocumentationContent): Boolean = content is DescriptorDocumentationContent

    override fun convertToHtml(content: DocumentationContent, docTagParserContext: DocTagParserContext): String {
        val descriptorContent = content as DescriptorDocumentationContent
        val inheritedDocNode = parser.parseDocumentation(
            KotlinDocComment(descriptorContent.element, descriptorContent.descriptor),
            parseWithChildren = false
        )
        val id = docTagParserContext.store(inheritedDocNode)
        return """<inheritdoc id="$id"/>"""
    }
}

internal data class DescriptorDocumentationContent(
    val descriptor: DeclarationDescriptor,
    val element: KDocTag,
    override val tag: JavadocTag,
) : DocumentationContent {
    override fun resolveSiblings(): List<DocumentationContent> {
        return listOf(this)
    }
}

class KotlinDocCommentCreator : DocCommentCreator {
    override fun create(element: PsiNamedElement): DocComment? {
        val ktElement = element.navigationElement as? KtElement ?: return null
        val kdoc = ktElement.findKDoc { DescriptorToSourceUtils.descriptorToDeclaration(it) } ?: return null
        val descriptor = (element.navigationElement as? KtDeclaration)?.descriptor ?: return null
        return KotlinDocComment(kdoc, descriptor)
    }
}

class KotlinDocComment(
    val comment: KDocTag,
    val descriptor: DeclarationDescriptor
) : DocComment {

    private val tagsWithContent: List<KDocTag> = comment.children.mapNotNull { (it as? KDocTag) }

    override fun hasTag(tag: JavadocTag): Boolean {
        return when (tag) {
            is DescriptionJavadocTag -> comment.getContent().isNotEmpty()
            is ThrowingExceptionJavadocTag -> tagsWithContent.any { it.hasException(tag) }
            else -> tagsWithContent.any { it.text.startsWith("@${tag.name}") }
        }
    }

    private fun KDocTag.hasException(tag: ThrowingExceptionJavadocTag) =
        text.startsWith("@${tag.name}") && getSubjectName() == tag.exceptionQualifiedName

    override fun resolveTag(tag: JavadocTag): List<DocumentationContent> {
        return when (tag) {
            is DescriptionJavadocTag -> listOf(DescriptorDocumentationContent(descriptor, comment, tag))
            is ParamJavadocTag -> {
                val resolvedContent = resolveGeneric(tag)
                listOf(resolvedContent[tag.paramIndex])
            }
            is ThrowsJavadocTag -> resolveThrowingException(tag)
            is ExceptionJavadocTag -> resolveThrowingException(tag)
            else -> resolveGeneric(tag)
        }
    }

    private fun resolveThrowingException(tag: ThrowingExceptionJavadocTag): List<DescriptorDocumentationContent> {
        val exceptionName = tag.exceptionQualifiedName ?: return resolveGeneric(tag)

        return comment.children
            .filterIsInstance<KDocTag>()
            .filter { it.name == tag.name && it.getSubjectName() == exceptionName }
            .map { DescriptorDocumentationContent(descriptor, it, tag) }
    }

    private fun resolveGeneric(tag: JavadocTag): List<DescriptorDocumentationContent> {
        return comment.children.mapNotNull { element ->
            if (element is KDocTag && element.name == tag.name) {
                DescriptorDocumentationContent(descriptor, element, tag)
            } else {
                null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KotlinDocComment

        if (comment != other.comment) return false
        if (descriptor != other.descriptor) return false
        if (tagsWithContent != other.tagsWithContent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = comment.hashCode()
        result = 31 * result + descriptor.hashCode()
        result = 31 * result + tagsWithContent.hashCode()
        return result
    }
}

class KotlinDocCommentParser(
    private val resolutionFacade: DokkaResolutionFacade,
    private val logger: DokkaLogger
) : DocCommentParser {

    override fun canParse(docComment: DocComment): Boolean {
        return docComment is KotlinDocComment
    }

    override fun parse(docComment: DocComment, context: PsiNamedElement): DocumentationNode {
        val kotlinDocComment = docComment as KotlinDocComment
        return parseDocumentation(kotlinDocComment)
    }

    fun parseDocumentation(element: KotlinDocComment, parseWithChildren: Boolean = true): DocumentationNode =
        MarkdownParser.parseFromKDocTag(
            kDocTag = element.comment,
            externalDri = { link: String ->
                try {
                    resolveKDocLink(
                        context = resolutionFacade.resolveSession.bindingContext,
                        resolutionFacade = resolutionFacade,
                        fromDescriptor = element.descriptor,
                        fromSubjectOfTag = null,
                        qualifiedName = link.split('.')
                    ).firstOrNull()?.let { DRI.from(it) }
                } catch (e1: IllegalArgumentException) {
                    logger.warn("Couldn't resolve link for $link")
                    null
                }
            },
            kdocLocation = null,
            parseWithChildren = parseWithChildren
        )
}
