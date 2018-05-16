package org.jetbrains.dokka.Kotlin

import com.google.inject.Inject
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.markdown.parser.LinkMap
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Samples.SampleProcessingService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

class DescriptorDocumentationParser
         @Inject constructor(val options: DocumentationOptions,
                             val logger: DokkaLogger,
                             val linkResolver: DeclarationLinkResolver,
                             val resolutionFacade: DokkaResolutionFacade,
                             val refGraph: NodeReferenceGraph,
                             val sampleService: SampleProcessingService)
{
    fun parseDocumentation(descriptor: DeclarationDescriptor, inline: Boolean = false): Content =
            parseDocumentationAndDetails(descriptor, inline).first

    fun parseDocumentationAndDetails(descriptor: DeclarationDescriptor, inline: Boolean = false): Pair<Content, (DocumentationNode) -> Unit> {
        if (descriptor is JavaClassDescriptor || descriptor is JavaCallableMemberDescriptor) {
            return parseJavadoc(descriptor)
        }

        val kdoc = descriptor.findKDoc() ?: findStdlibKDoc(descriptor)
        if (kdoc == null) {
            if (options.effectivePackageOptions(descriptor.fqNameSafe).reportUndocumented && !descriptor.isDeprecated() &&
                    descriptor !is ValueParameterDescriptor && descriptor !is TypeParameterDescriptor &&
                    descriptor !is PropertyAccessorDescriptor && !descriptor.isSuppressWarning()) {
                logger.warn("No documentation for ${descriptor.signatureWithSourceLocation()}")
            }
            return Content.Empty to { node -> }
        }

        val contextDescriptor =
            (PsiTreeUtil.getParentOfType(kdoc, KDoc::class.java)?.context as? KtDeclaration)
                ?.takeIf { it != descriptor.original.sourcePsi() }
                ?.resolveToDescriptorIfAny()
                ?: descriptor

        var kdocText = kdoc.getContent()
        // workaround for code fence parsing problem in IJ markdown parser
        if (kdocText.endsWith("```") || kdocText.endsWith("~~~")) {
            kdocText += "\n"
        }
        val tree = parseMarkdown(kdocText)
        val linkMap = LinkMap.buildLinkMap(tree.node, kdocText)
        val content = buildContent(tree, LinkResolver(linkMap, { href -> linkResolver.resolveContentLink(contextDescriptor, href) }), inline)
        if (kdoc is KDocSection) {
            val tags = kdoc.getTags()
            tags.forEach {
                when (it.knownTag) {
                    KDocKnownTag.SAMPLE ->
                        content.append(sampleService.resolveSample(contextDescriptor, it.getSubjectName(), it))
                    KDocKnownTag.SEE ->
                        content.addTagToSeeAlso(contextDescriptor, it)
                    else -> {
                        val section = content.addSection(javadocSectionDisplayName(it.name), it.getSubjectName())
                        val sectionContent = it.getContent()
                        val markdownNode = parseMarkdown(sectionContent)
                        buildInlineContentTo(markdownNode, section, LinkResolver(linkMap, { href -> linkResolver.resolveContentLink(contextDescriptor, href) }))
                    }
                }
            }
        }
        return content to { node -> }
    }

    private fun DeclarationDescriptor.isSuppressWarning() : Boolean {
        val suppressAnnotation = annotations.findAnnotation(FqName(Suppress::class.qualifiedName!!))
        return if (suppressAnnotation != null) {
            @Suppress("UNCHECKED_CAST")
            (suppressAnnotation.argumentValue("names")?.value as List<StringValue>).any { it.value == "NOT_DOCUMENTED" }
        } else containingDeclaration?.isSuppressWarning() ?: false
    }

    /**
     * Special case for generating stdlib documentation (the Any class to which the override chain will resolve
     * is not the same one as the Any class included in the source scope).
     */
    fun findStdlibKDoc(descriptor: DeclarationDescriptor): KDocTag? {
        if (descriptor !is CallableMemberDescriptor) {
            return null
        }
        val name = descriptor.name.asString()
        if (name == "equals" || name == "hashCode" || name == "toString") {
            var deepestDescriptor: CallableMemberDescriptor = descriptor
            while (!deepestDescriptor.overriddenDescriptors.isEmpty()) {
                deepestDescriptor = deepestDescriptor.overriddenDescriptors.first()
            }
            if (DescriptorUtils.getFqName(deepestDescriptor.containingDeclaration).asString() == "kotlin.Any") {
                val anyClassDescriptors = resolutionFacade.resolveSession.getTopLevelClassifierDescriptors(
                        FqName.fromSegments(listOf("kotlin", "Any")), NoLookupLocation.FROM_IDE)
                anyClassDescriptors.forEach {
                    val anyMethod = (it as ClassDescriptor).getMemberScope(listOf())
                            .getDescriptorsFiltered(DescriptorKindFilter.FUNCTIONS, { it == descriptor.name })
                            .single()
                    val kdoc = anyMethod.findKDoc()
                    if (kdoc != null) {
                        return kdoc
                    }
                }
            }
        }
        return null
    }

    fun parseJavadoc(descriptor: DeclarationDescriptor): Pair<Content, (DocumentationNode) -> Unit> {
        val psi = ((descriptor as? DeclarationDescriptorWithSource)?.source as? PsiSourceElement)?.psi
        if (psi is PsiDocCommentOwner) {
            val parseResult = JavadocParser(refGraph, logger).parseDocumentation(psi as PsiNamedElement)
            return parseResult.content to { node ->
                parseResult.deprecatedContent?.let {
                    val deprecationNode = DocumentationNode("", it, NodeKind.Modifier)
                    node.append(deprecationNode, RefKind.Deprecation)
                }
            }
        }
        return Content.Empty to { node -> }
    }

    fun KDocSection.getTags(): Array<KDocTag> = PsiTreeUtil.getChildrenOfType(this, KDocTag::class.java) ?: arrayOf()

    private fun MutableContent.addTagToSeeAlso(descriptor: DeclarationDescriptor, seeTag: KDocTag) {
        val subjectName = seeTag.getSubjectName()
        if (subjectName != null) {
            val seeSection = findSectionByTag("See Also") ?: addSection("See Also", null)
            val link = linkResolver.resolveContentLink(descriptor, subjectName)
            link.append(ContentText(subjectName))
            val para = ContentParagraph()
            para.append(link)
            seeSection.append(para)
        }
    }

}
