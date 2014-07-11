package com.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.impl.*

fun BindingContext.createDocumentation(file: JetFile): DocumentationModel {
    val model = DocumentationModel()
    val packageFragment = getPackageFragment(file)
    if (packageFragment == null) throw IllegalArgumentException("File $file should have package fragment")

    val visitor = DocumentationBuilderVisitor()
    visitDescriptor(packageFragment, model, visitor)

    return model
}

class DocumentationBuilderVisitor() : DeclarationDescriptorVisitorEmptyBodies<DocumentationNode, DocumentationNode>() {

    override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!.getName().asString(), "doc", DocumentationNodeKind.Function)
        data?.addReferenceTo(node, DocumentationReferenceKind.Member)
        return node
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!.getName().asString(), "doc", DocumentationNodeKind.Function)
        data?.addReferenceTo(node, DocumentationReferenceKind.Detail)
        return node
    }
}
