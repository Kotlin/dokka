package org.jetbrains.dokka.Kotlin

import com.google.inject.Inject
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.markdown.parser.LinkMap
import org.jetbrains.dokka.*
import org.jetbrains.dokka.Samples.SampleProcessingService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import java.util.regex.Pattern

private val REF_COMMAND = "ref"
private val NAME_COMMAND = "name"
private val DESCRIPTION_COMMAND = "description"
private val TEXT = Pattern.compile("(\\S+)\\s*(.*)", Pattern.DOTALL)
private val NAME_TEXT = Pattern.compile("(\\S+)(.*)", Pattern.DOTALL)

class DescriptorDocumentationParser @Inject constructor(
        val options: DocumentationOptions,
        val logger: DokkaLogger,
        val linkResolver: DeclarationLinkResolver,
        val resolutionFacade: DokkaResolutionFacade,
        val refGraph: NodeReferenceGraph,
        val sampleService: SampleProcessingService
) {

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
        var kdocText = kdoc.getContent()
        // workaround for code fence parsing problem in IJ markdown parser
        if (kdocText.endsWith("```") || kdocText.endsWith("~~~")) {
            kdocText += "\n"
        }
        val tree = parseMarkdown(kdocText)
        val linkMap = LinkMap.buildLinkMap(tree.node, kdocText)
        val content = buildContent(tree, LinkResolver(linkMap, { href -> linkResolver.resolveContentLink(descriptor, href) }), inline)
        if (kdoc is KDocSection) {
            val tags = kdoc.getTags()
            tags.forEach {
                when (it.knownTag) {
                    KDocKnownTag.SAMPLE ->
                        content.append(sampleService.resolveSample(descriptor, it.getSubjectName(), it))
                    KDocKnownTag.SEE ->
                        content.addTagToSeeAlso(descriptor, it)
                    else -> {
                        val section = content.addSection(javadocSectionDisplayName(it.name), it.getSubjectName())
                        val sectionContent = it.getContent()
                        val markdownNode = parseMarkdown(sectionContent)
                        buildInlineContentTo(markdownNode, section, LinkResolver(linkMap, { href -> linkResolver.resolveContentLink(descriptor, href) }))
                    }
                }
            }
        }
        return content to { node ->
            if (kdoc is KDocSection) {
                val tags = kdoc.getTags()
                node.addExtraTags(tags, descriptor)
            }
        }
    }

    /**
     * For now, this only adds @attr tag. There are 3 types of syntax for this:
     * @attr ref <android.>R.styleable.<attribute_name>
     * @attr name <attribute_name>
     * @attr description <attribute_description>
     */
    private fun DocumentationNode.addExtraTags(tags: Array<KDocTag>, descriptor: DeclarationDescriptor) {
        tags.forEach {
            val name = it.name
            if (name?.toLowerCase() == "attr") {
                val matcher = TEXT.matcher(it.getContent())
                if (matcher.matches()) {
                    val command = matcher.group(1)
                    val more = matcher.group(2)
                    val attribute: DocumentationNode? = when (command) {
                        REF_COMMAND -> {
                            val attrRef = more.trim()
                            val qualified = attrRef.split('.', '#')
                            val targetDescriptor = resolveKDocLink(resolutionFacade.resolveSession.bindingContext, resolutionFacade, descriptor, it, qualified)
                            DocumentationNode(attrRef, Content.Empty, NodeKind.Attribute).also {
                                if (targetDescriptor.isNotEmpty()) {
                                    refGraph.link(it, targetDescriptor.first().signature(), RefKind.Attribute)
                                }
                            }
                        }
                        NAME_COMMAND -> {
                            val nameMatcher = NAME_TEXT.matcher(more)
                            if (nameMatcher.matches()) {
                                val attrName = nameMatcher.group(1)
                                DocumentationNode(attrName, Content.Empty, NodeKind.Attribute)
                            } else {
                                null
                            }
                        }
                        DESCRIPTION_COMMAND -> {
                            val attrDescription = more
                            DocumentationNode(attrDescription, Content.Empty, NodeKind.Attribute)
                        }
                        else -> null
                    }
                    attribute?.let { append(it, RefKind.Attribute) }
                }
            } else if (name?.toLowerCase() == "since") {
                val apiLevel = DocumentationNode(it.getContent(), Content.Empty, NodeKind.ApiLevel)
                append(apiLevel, RefKind.Detail)
            }
        }
    }

    private fun DeclarationDescriptor.isSuppressWarning(): Boolean {
        val suppressAnnotation = annotations.findAnnotation(FqName(Suppress::class.qualifiedName!!))
        return if (suppressAnnotation != null) {
            @Suppress("UNCHECKED_CAST")
            (suppressAnnotation.argumentValue("names") as List<StringValue>).any { it.value == "NOT_DOCUMENTED" }
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

    fun KDocSection.getTags(): Array<KDocTag> = PsiTreeUtil.getChildrenOfType(this, KDocTag::class.java)
            ?: arrayOf()

    private fun MutableContent.addTagToSeeAlso(descriptor: DeclarationDescriptor, seeTag: KDocTag) {
        addTagToSection(seeTag, descriptor, "See Also")
    }

    private fun MutableContent.addTagToSection(seeTag: KDocTag, descriptor: DeclarationDescriptor, sectionName: String) {
        val subjectName = seeTag.getSubjectName()
        if (subjectName != null) {
            val section = findSectionByTag(sectionName) ?: addSection(sectionName, null)
            val link = linkResolver.resolveContentLink(descriptor, subjectName)
            link.append(ContentText(subjectName))
            val para = ContentParagraph()
            para.append(link)
            section.append(para)
        }
    }

}
