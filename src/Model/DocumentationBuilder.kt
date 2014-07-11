package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.impl.*

fun BindingContext.createSourceModel(file: JetFile): DocumentationModel {
    val model = DocumentationModel()
    val packageFragment = getPackageFragment(file)
    if (packageFragment == null) throw IllegalArgumentException("File $file should have package fragment")

    val visitor = DocumentationBuilderVisitor(this)
    visitDescriptor(packageFragment, model, visitor)

    return model
}

class DocumentationBuilderVisitor(val context: BindingContext) : DeclarationDescriptorVisitorEmptyBodies<DocumentationNode, DocumentationNode>() {

    override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.Unknown)
        data?.addReferenceTo(node, DocumentationReferenceKind.Member)
        return node
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.Parameter)
        data?.addReferenceTo(node, DocumentationReferenceKind.Detail)
        return node
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc,
                                     when (descriptor.getKind()) {
                                         ClassKind.OBJECT -> DocumentationNodeKind.Object
                                         else -> DocumentationNodeKind.Class
                                     }
                                    )
        data?.addReferenceTo(node, DocumentationReferenceKind.Member)
        return node
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.Function)
        data?.addReferenceTo(node, DocumentationReferenceKind.Member)
        return node
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.TypeParameter)
        data?.addReferenceTo(node, DocumentationReferenceKind.Detail)
        return node
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.Property)
        data?.addReferenceTo(node, DocumentationReferenceKind.Member)
        return node
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.Constructor)
        data?.addReferenceTo(node, DocumentationReferenceKind.Member)
        return node
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!.fqName.asString(), "", DocumentationNodeKind.Package)
        data?.addReferenceTo(node, DocumentationReferenceKind.Member)
        return node
    }
}
