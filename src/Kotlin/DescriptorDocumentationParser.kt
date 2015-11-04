package org.jetbrains.dokka.Kotlin

import com.google.inject.Inject
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.kdoc.KDocFinder
import org.jetbrains.kotlin.idea.kdoc.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.resolve.scopes.utils.asJetScope
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

class DescriptorDocumentationParser
         @Inject constructor(val options: DocumentationOptions,
                             val logger: DokkaLogger,
                             val linkResolver: DeclarationLinkResolver,
                             val resolutionFacade: DokkaResolutionFacade,
                             val refGraph: NodeReferenceGraph)
{
    fun parseDocumentation(descriptor: DeclarationDescriptor, inline: Boolean = false): Content =
            parseDocumentationAndDetails(descriptor, inline).first

    fun parseDocumentationAndDetails(descriptor: DeclarationDescriptor, inline: Boolean = false): Pair<Content, (DocumentationNode) -> Unit> {
        if (descriptor is JavaClassDescriptor || descriptor is JavaCallableMemberDescriptor) {
            return parseJavadoc(descriptor)
        }

        val kdoc = KDocFinder.findKDoc(descriptor) ?: findStdlibKDoc(descriptor)
        if (kdoc == null) {
            if (options.reportUndocumented && !descriptor.isDeprecated() &&
                    descriptor !is ValueParameterDescriptor && descriptor !is TypeParameterDescriptor &&
                    descriptor !is PropertyAccessorDescriptor) {
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
        val content = buildContent(tree, { href -> linkResolver.resolveContentLink(descriptor, href) }, inline)
        if (kdoc is KDocSection) {
            val tags = kdoc.getTags()
            tags.forEach {
                when (it.name) {
                    "sample" ->
                        content.append(functionBody(descriptor, it.getSubjectName()))
                    "see" ->
                        content.addTagToSeeAlso(descriptor, it)
                    else -> {
                        val section = content.addSection(javadocSectionDisplayName(it.name), it.getSubjectName())
                        val sectionContent = it.getContent()
                        val markdownNode = parseMarkdown(sectionContent)
                        buildInlineContentTo(markdownNode, section, { href -> linkResolver.resolveContentLink(descriptor, href) })
                    }
                }
            }
        }
        return content to { node -> }
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
                val anyClassDescriptors = resolutionFacade.resolveSession.getTopLevelClassDescriptors(
                        FqName.fromSegments(listOf("kotlin", "Any")), NoLookupLocation.UNSORTED)
                anyClassDescriptors.forEach {
                    val anyMethod = it.getMemberScope(listOf()).getFunctions(descriptor.name, NoLookupLocation.UNSORTED).single()
                    val kdoc = KDocFinder.findKDoc(anyMethod)
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
            val parseResult = JavadocParser(refGraph).parseDocumentation(psi as PsiNamedElement)
            return parseResult.content to { node ->
                parseResult.deprecatedContent?.let {
                    val deprecationNode = DocumentationNode("", it, DocumentationNode.Kind.Modifier)
                    node.append(deprecationNode, DocumentationReference.Kind.Deprecation)
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

    private fun functionBody(descriptor: DeclarationDescriptor, functionName: String?): ContentNode {
        if (functionName == null) {
            logger.warn("Missing function name in @sample in ${descriptor.signature()}")
            return ContentBlockCode().let() { it.append(ContentText("Missing function name in @sample")); it }
        }
        val scope = getResolutionScope(resolutionFacade, descriptor).asJetScope()
        val rootPackage = resolutionFacade.moduleDescriptor.getPackage(FqName.ROOT)
        val rootScope = rootPackage.memberScope
        val symbol = resolveInScope(functionName, scope) ?: resolveInScope(functionName, rootScope)
        if (symbol == null) {
            logger.warn("Unresolved function $functionName in @sample in ${descriptor.signature()}")
            return ContentBlockCode().let() { it.append(ContentText("Unresolved: $functionName")); it }
        }
        val psiElement = DescriptorToSourceUtils.descriptorToDeclaration(symbol)
        if (psiElement == null) {
            logger.warn("Can't find source for function $functionName in @sample in ${descriptor.signature()}")
            return ContentBlockCode().let() { it.append(ContentText("Source not found: $functionName")); it }
        }

        val text = when (psiElement) {
            is KtDeclarationWithBody -> ContentBlockCode().let() {
                val bodyExpression = psiElement.bodyExpression
                when (bodyExpression) {
                    is KtBlockExpression -> bodyExpression.text.removeSurrounding("{", "}")
                    else -> bodyExpression!!.text
                }
            }
            else -> psiElement.text
        }

        val lines = text.trimEnd().split("\n".toRegex()).toTypedArray().filterNot { it.length == 0 }
        val indent = lines.map { it.takeWhile { it.isWhitespace() }.count() }.min() ?: 0
        val finalText = lines.map { it.drop(indent) }.joinToString("\n")
        return ContentBlockCode("kotlin").let() { it.append(ContentText(finalText)); it }
    }

    private fun resolveInScope(functionName: String, scope: KtScope): DeclarationDescriptor? {
        var currentScope = scope
        val parts = functionName.split('.')

        var symbol: DeclarationDescriptor? = null

        for (part in parts) {
            // short name
            val symbolName = Name.guess(part)
            val partSymbol = currentScope.getAllDescriptors().filter { it.name == symbolName }.firstOrNull()

            if (partSymbol == null) {
                symbol = null
                break
            }
            currentScope = if (partSymbol is ClassDescriptor)
                partSymbol.defaultType.memberScope
            else
                getResolutionScope(resolutionFacade, partSymbol).asJetScope()
            symbol = partSymbol
        }

        return symbol
    }
}
