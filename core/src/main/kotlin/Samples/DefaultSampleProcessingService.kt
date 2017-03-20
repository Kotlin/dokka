package org.jetbrains.dokka.Samples

import com.google.inject.Inject
import com.intellij.psi.PsiElement
import org.jetbrains.dokka.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.idea.kdoc.getKDocLinkResolutionScope
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope


open class DefaultSampleProcessingService
@Inject constructor(val options: DocumentationOptions,
                    val logger: DokkaLogger,
                    val resolutionFacade: DokkaResolutionFacade)
    : SampleProcessingService {

    override fun resolveSample(descriptor: DeclarationDescriptor, functionName: String?, kdocTag: KDocTag): ContentNode {
        if (functionName == null) {
            logger.warn("Missing function name in @sample in ${descriptor.signature()}")
            return ContentBlockSampleCode().apply { append(ContentText("//Missing function name in @sample")) }
        }
        val bindingContext = BindingContext.EMPTY
        val symbol = resolveKDocLink(bindingContext, resolutionFacade, descriptor, kdocTag, functionName.split(".")).firstOrNull()
        if (symbol == null) {
            logger.warn("Unresolved function $functionName in @sample in ${descriptor.signature()}")
            return ContentBlockSampleCode().apply { append(ContentText("//Unresolved: $functionName")) }
        }
        val psiElement = DescriptorToSourceUtils.descriptorToDeclaration(symbol)
        if (psiElement == null) {
            logger.warn("Can't find source for function $functionName in @sample in ${descriptor.signature()}")
            return ContentBlockSampleCode().apply { append(ContentText("//Source not found: $functionName")) }
        }

        val text = processSampleBody(psiElement).trim { it == '\n' || it == '\r' }.trimEnd()
        val lines = text.split("\n")
        val indent = lines.filter(String::isNotBlank).map { it.takeWhile(Char::isWhitespace).count() }.min() ?: 0
        val finalText = lines.map { it.drop(indent) }.joinToString("\n")

        return ContentBlockSampleCode(importsBlock = processImports(psiElement)).apply { append(ContentText(finalText)) }
    }

    protected open fun processSampleBody(psiElement: PsiElement): String = when (psiElement) {
        is KtDeclarationWithBody -> {
            val bodyExpression = psiElement.bodyExpression
            when (bodyExpression) {
                is KtBlockExpression -> bodyExpression.text.removeSurrounding("{", "}")
                else -> bodyExpression!!.text
            }
        }
        else -> psiElement.text
    }

    protected open fun processImports(psiElement: PsiElement): ContentBlockCode {
        val psiFile = psiElement.containingFile
        if (psiFile is KtFile) {
            return ContentBlockCode("kotlin").apply {
                append(ContentText(psiFile.importList?.text ?: ""))
            }
        } else {
            return ContentBlockCode("")
        }
    }

    private fun resolveInScope(functionName: String, scope: ResolutionScope): DeclarationDescriptor? {
        var currentScope = scope
        val parts = functionName.split('.')

        var symbol: DeclarationDescriptor? = null

        for (part in parts) {
            // short name
            val symbolName = Name.identifier(part)
            val partSymbol = currentScope.getContributedDescriptors(DescriptorKindFilter.ALL, { it == symbolName })
                    .filter { it.name == symbolName }
                    .firstOrNull()

            if (partSymbol == null) {
                symbol = null
                break
            }
            @Suppress("IfThenToElvis")
            currentScope = if (partSymbol is ClassDescriptor)
                partSymbol.defaultType.memberScope
            else if (partSymbol is PackageViewDescriptor)
                partSymbol.memberScope
            else
                getKDocLinkResolutionScope(resolutionFacade, partSymbol)
            symbol = partSymbol
        }

        return symbol
    }
}

