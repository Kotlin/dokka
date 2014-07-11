package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.impl.*
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

class DocumentationNodeBuilder(val context: BindingContext) : DeclarationDescriptorVisitorEmptyBodies<DocumentationNode, DocumentationNode>() {

    override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.Unknown, context.getResolutionScope(descriptor))
        data!!.addReferenceTo(node, DocumentationReferenceKind.Link)
        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!.getName().asString(), DocumentationContent.Empty, DocumentationNodeKind.Receiver, context.getResolutionScope(descriptor))
        data!!.addReferenceTo(node, DocumentationReferenceKind.Detail)

        val typeNode = DocumentationNode(descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNodeKind.Type, context.getResolutionScope(descriptor))
        node.addReferenceTo(typeNode, DocumentationReferenceKind.Detail)

        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.Parameter, context.getResolutionScope(descriptor))
        data!!.addReferenceTo(node, DocumentationReferenceKind.Detail)

        val typeNode = DocumentationNode(descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNodeKind.Type, context.getResolutionScope(descriptor))
        node.addReferenceTo(typeNode, DocumentationReferenceKind.Detail)

        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, when (descriptor.getKind()) {
            ClassKind.OBJECT -> DocumentationNodeKind.Object
            ClassKind.TRAIT -> DocumentationNodeKind.Trait
            else -> DocumentationNodeKind.Class
        }, context.getResolutionScope(descriptor))
        data!!.addReferenceTo(node, DocumentationReferenceKind.Member)
        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.Function, context.getResolutionScope(descriptor))
        data!!.addReferenceTo(node, DocumentationReferenceKind.Member)

        val typeNode = DocumentationNode(descriptor.getReturnType().toString(), DocumentationContent.Empty, DocumentationNodeKind.Type, context.getResolutionScope(descriptor))
        node.addReferenceTo(typeNode, DocumentationReferenceKind.Detail)

        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.TypeParameter, context.getResolutionScope(descriptor))
        data!!.addReferenceTo(node, DocumentationReferenceKind.Detail)
        val builtIns = KotlinBuiltIns.getInstance()
        for (constraint in descriptor.getUpperBounds()) {
            if (constraint == builtIns.getDefaultBound())
                continue
            val constraintNode = DocumentationNode(constraint.toString(), DocumentationContent.Empty, DocumentationNodeKind.UpperBound, context.getResolutionScope(descriptor))
            node.addReferenceTo(constraintNode, DocumentationReferenceKind.Detail)
        }
        for (constraint in descriptor.getLowerBounds()) {
            if (builtIns.isNothing(constraint))
                continue
            val constraintNode = DocumentationNode(constraint.toString(), DocumentationContent.Empty, DocumentationNodeKind.LowerBound, context.getResolutionScope(descriptor))
            node.addReferenceTo(constraintNode, DocumentationReferenceKind.Detail)
        }
        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.Property, context.getResolutionScope(descriptor))
        data!!.addReferenceTo(node, DocumentationReferenceKind.Member)

        val typeNode = DocumentationNode(descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNodeKind.Type, context.getResolutionScope(descriptor))
        node.addReferenceTo(typeNode, DocumentationReferenceKind.Detail)

        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNodeKind.Constructor, context.getResolutionScope(descriptor))
        data!!.addReferenceTo(node, DocumentationReferenceKind.Member)
        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!.fqName.asString(), DocumentationContent.Empty, DocumentationNodeKind.Package, context.getResolutionScope(descriptor))
        data!!.addReferenceTo(node, DocumentationReferenceKind.Member)
        node.addReferenceTo(data, DocumentationReferenceKind.Owner)
        return node
    }
}
