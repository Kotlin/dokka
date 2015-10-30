package org.jetbrains.dokka

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import org.jetbrains.kotlin.asJava.KotlinLightElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName

class KotlinAsJavaDocumentationBuilder() : PackageDocumentationBuilder {
    override fun buildPackageDocumentation(project: Project,
                                           packageName: FqName,
                                           packageNode: DocumentationNode,
                                           declarations: List<DeclarationDescriptor>,
                                           options: DocumentationOptions,
                                           refGraph: NodeReferenceGraph,
                                           logger: DokkaLogger) {
        val psiPackage = JavaPsiFacade.getInstance(project).findPackage(packageName.asString())
        if (psiPackage == null) {
            logger.error("Cannot find Java package by qualified name: ${packageName.asString()}")
            return
        }
        val javaDocumentationBuilder = JavaDocumentationBuilder(options, refGraph)
        psiPackage.classes.filter { it is KotlinLightElement<*, *> }.forEach {
            javaDocumentationBuilder.appendClasses(packageNode, arrayOf(it))
        }
    }
}
