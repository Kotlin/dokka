package org.jetbrains.dokka

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.asJava.KotlinLightElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPropertyAccessor

class KotlinAsJavaDocumentationBuilder() : PackageDocumentationBuilder {
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

        val javaDocumentationBuilder = JavaDocumentationBuilder(documentationBuilder.options,
                documentationBuilder.refGraph,
                KotlinAsJavaDocumentationParser(documentationBuilder))

        psiPackage.classes.filter { it is KotlinLightElement<*, *> }.forEach {
            javaDocumentationBuilder.appendClasses(packageNode, arrayOf(it))
        }
    }
}

class KotlinAsJavaDocumentationParser(val documentationBuilder: DocumentationBuilder) : JavaDocumentationParser {
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
        val descriptor = documentationBuilder.session.resolveToDescriptor(origin)
        val content = documentationBuilder.parseDocumentation(descriptor)
        return JavadocParseResult(content, null)
    }
}
