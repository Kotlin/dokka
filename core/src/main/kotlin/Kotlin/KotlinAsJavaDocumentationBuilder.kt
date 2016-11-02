package org.jetbrains.dokka

import com.google.inject.Inject
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.Kotlin.DescriptorDocumentationParser
import org.jetbrains.kotlin.asJava.KtLightElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPropertyAccessor

class KotlinAsJavaDocumentationBuilder
        @Inject constructor(val kotlinAsJavaDocumentationParser: KotlinAsJavaDocumentationParser) : PackageDocumentationBuilder
{
    override fun buildPackageDocumentation(documentationBuilder: DocumentationBuilder,
                                           packageName: FqName,
                                           packageNode: DocumentationNode,
                                           declarations: List<DeclarationDescriptor>,
                                           allFqNames: Collection<FqName>) {
        val project = documentationBuilder.resolutionFacade.project
        val psiPackage = JavaPsiFacade.getInstance(project).findPackage(packageName.asString())
        if (psiPackage == null) {
            documentationBuilder.logger.error("Cannot find Java package by qualified name: ${packageName.asString()}")
            return
        }

        val javaDocumentationBuilder = JavaPsiDocumentationBuilder(documentationBuilder.options,
                documentationBuilder.refGraph,
                kotlinAsJavaDocumentationParser)

        psiPackage.classes.filter { it is KtLightElement<*, *> }.filter { it.isVisibleInDocumentation() }.forEach {
            javaDocumentationBuilder.appendClasses(packageNode, arrayOf(it))
        }
    }

    fun KtDeclaration.isNotSuppressed() =
            PsiTreeUtil.findChildrenOfType(this.docComment, KDocTag::class.java).none { it.knownTag == KDocKnownTag.SUPPRESS }

    fun PsiClass.isVisibleInDocumentation() : Boolean {
        val origin: KtDeclaration = (this as KtLightElement<*, *>).kotlinOrigin as? KtDeclaration ?: return true

        return  origin.isNotSuppressed() &&
                origin.hasModifier(KtTokens.INTERNAL_KEYWORD) != true &&
               origin.hasModifier(KtTokens.PRIVATE_KEYWORD) != true
    }
}

class KotlinAsJavaDocumentationParser
        @Inject constructor(val resolutionFacade: DokkaResolutionFacade,
                            val descriptorDocumentationParser: DescriptorDocumentationParser) : JavaDocumentationParser
{
    override fun parseDocumentation(element: PsiNamedElement): JavadocParseResult {
        val kotlinLightElement = element as? KtLightElement<*, *> ?: return JavadocParseResult.Empty
        val origin = kotlinLightElement.kotlinOrigin as? KtDeclaration ?: return JavadocParseResult.Empty
        if (origin is KtParameter) {
            // LazyDeclarationResolver does not support setter parameters
            val grandFather = origin.parent?.parent
            if (grandFather is KtPropertyAccessor) {
                return JavadocParseResult.Empty
            }
        }
        val descriptor = resolutionFacade.resolveToDescriptor(origin)
        val content = descriptorDocumentationParser.parseDocumentation(descriptor, origin is KtParameter)
        return JavadocParseResult(content, null)
    }
}
