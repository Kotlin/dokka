package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.impl.*
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

class DocumentationNodeBuilder(val context: BindingContext) : DeclarationDescriptorVisitorEmptyBodies<DocumentationNode, DocumentationNode>() {

    override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNodeKind.Unknown)
        data!!.addReferenceTo(node, DocumentationReferenceKind.Link)
        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!, descriptor.getName().asString(), DocumentationContent.Empty, DocumentationNodeKind.Receiver)
        data!!.addReferenceTo(node, DocumentationReferenceKind.Detail)

        val typeNode = DocumentationNode(descriptor, descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNodeKind.Type)
        node.addReferenceTo(typeNode, DocumentationReferenceKind.Detail)

        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNodeKind.Parameter)
        data!!.addReferenceTo(node, DocumentationReferenceKind.Detail)

        val typeNode = DocumentationNode(descriptor, descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNodeKind.Type)
        node.addReferenceTo(typeNode, DocumentationReferenceKind.Detail)

        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, when (descriptor.getKind()) {
            ClassKind.OBJECT -> DocumentationNodeKind.Object
            ClassKind.TRAIT -> DocumentationNodeKind.Trait
            else -> DocumentationNodeKind.Class
        })
        data!!.addReferenceTo(node, DocumentationReferenceKind.Member)
        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNodeKind.Function)
        data!!.addReferenceTo(node, DocumentationReferenceKind.Member)

        val typeNode = DocumentationNode(descriptor, descriptor.getReturnType().toString(), DocumentationContent.Empty, DocumentationNodeKind.Type)
        node.addReferenceTo(typeNode, DocumentationReferenceKind.Detail)

        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNodeKind.TypeParameter)
        data!!.addReferenceTo(node, DocumentationReferenceKind.Detail)
        val builtIns = KotlinBuiltIns.getInstance()
        for (constraint in descriptor.getUpperBounds()) {
            if (constraint == builtIns.getDefaultBound())
                continue
            val constraintNode = DocumentationNode(descriptor, constraint.toString(), DocumentationContent.Empty, DocumentationNodeKind.UpperBound)
            node.addReferenceTo(constraintNode, DocumentationReferenceKind.Detail)
        }
        for (constraint in descriptor.getLowerBounds()) {
            if (builtIns.isNothing(constraint))
                continue
            val constraintNode = DocumentationNode(descriptor, constraint.toString(), DocumentationContent.Empty, DocumentationNodeKind.LowerBound)
            node.addReferenceTo(constraintNode, DocumentationReferenceKind.Detail)
        }
        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNodeKind.Property)
        data!!.addReferenceTo(node, DocumentationReferenceKind.Member)

        val typeNode = DocumentationNode(descriptor, descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNodeKind.Type)
        node.addReferenceTo(typeNode, DocumentationReferenceKind.Detail)

        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNodeKind.Constructor)
        data!!.addReferenceTo(node, DocumentationReferenceKind.Member)
        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!, descriptor.fqName.asString(), DocumentationContent.Empty, DocumentationNodeKind.Package)
        data!!.addReferenceTo(node, DocumentationReferenceKind.Member)
        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }
}
