package org.jetbrains.dokka

import com.google.inject.Inject
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.Kotlin.DescriptorDocumentationParser
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
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

        val javaDocumentationBuilder = JavaPsiDocumentationBuilder(documentationBuilder.passConfiguration,
                documentationBuilder.refGraph,
                kotlinAsJavaDocumentationParser)

        psiPackage.classes.filter { it is KtLightElement<*, *> }.filter { it.isVisibleInDocumentation() }.forEach {
            javaDocumentationBuilder.appendClasses(packageNode, arrayOf(it))
        }
    }

    fun PsiClass.isVisibleInDocumentation(): Boolean {
        val origin: KtDeclaration = (this as KtLightElement<*, *>).kotlinOrigin as? KtDeclaration ?: return true

        return !origin.hasModifier(KtTokens.INTERNAL_KEYWORD) && !origin.hasModifier(KtTokens.PRIVATE_KEYWORD)
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
        val isDefaultNoArgConstructor = kotlinLightElement is KtLightMethod && origin is KtClass
        val descriptor = resolutionFacade.resolveToDescriptor(origin)
        val content = descriptorDocumentationParser.parseDocumentation(descriptor, origin is KtParameter, isDefaultNoArgConstructor)
        return JavadocParseResult(content, null)
    }
}
