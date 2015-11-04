package org.jetbrains.dokka

import com.google.inject.Inject
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.Kotlin.DescriptorDocumentationParser
import org.jetbrains.kotlin.asJava.KotlinLightElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPropertyAccessor

class KotlinAsJavaDocumentationBuilder
        @Inject constructor(val kotlinAsJavaDocumentationParser: KotlinAsJavaDocumentationParser) : PackageDocumentationBuilder
{
    override fun buildPackageDocumentation(documentationBuilder: DocumentationBuilder,
                                           packageName: FqName,
                                           packageNode: DocumentationNode,
                                           declarations: List<DeclarationDescriptor>) {
        val project = documentationBuilder.resolutionFacade.project
        val psiPackage = JavaPsiFacade.getInstance(project).findPackage(packageName.asString())
        if (psiPackage == null) {
            documentationBuilder.logger.error("Cannot find Java package by qualified name: ${packageName.asString()}")
            return
        }

        val javaDocumentationBuilder = JavaPsiDocumentationBuilder(documentationBuilder.options,
                documentationBuilder.refGraph,
                kotlinAsJavaDocumentationParser)

        psiPackage.classes.filter { it is KotlinLightElement<*, *> }.forEach {
            javaDocumentationBuilder.appendClasses(packageNode, arrayOf(it))
        }
    }
}

class KotlinAsJavaDocumentationParser
        @Inject constructor(val resolutionFacade: DokkaResolutionFacade,
                            val descriptorDocumentationParser: DescriptorDocumentationParser) : JavaDocumentationParser
{
    override fun parseDocumentation(element: PsiNamedElement): JavadocParseResult {
        val kotlinLightElement = element as? KotlinLightElement<*, *> ?: return JavadocParseResult.Empty
        val origin = kotlinLightElement.getOrigin() ?: return JavadocParseResult.Empty
        if (origin is KtParameter) {
            // LazyDeclarationResolver does not support setter parameters
            val grandFather = origin.parent?.parent
            if (grandFather is KtPropertyAccessor) {
                return JavadocParseResult.Empty
            }
        }
        val descriptor = resolutionFacade.resolveToDescriptor(origin)
        val content = descriptorDocumentationParser.parseDocumentation(descriptor)
        return JavadocParseResult(content, null)
    }
}
