package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.impl.*
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

class DocumentationNodeBuilder(val context: BindingContext) : DeclarationDescriptorVisitorEmptyBodies<DocumentationNode, DocumentationNode>() {

    override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.Unknown)
        data!!.addReferenceTo(node, DocumentationReference.Kind.Link)
        node.addReferenceTo(data, DocumentationReference.Kind.Owner)
        return node
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!, descriptor.getName().asString(), DocumentationContent.Empty, DocumentationNode.Kind.Receiver)
        data!!.addReferenceTo(node, DocumentationReference.Kind.Detail)

        val typeNode = DocumentationNode(descriptor, descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNode.Kind.Type)
        node.addReferenceTo(typeNode, DocumentationReference.Kind.Detail)

        node.addReferenceTo(data, DocumentationReference.Kind.Owner)
        return node
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.Parameter)
        data!!.addReferenceTo(node, DocumentationReference.Kind.Detail)

        val typeNode = DocumentationNode(descriptor, descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNode.Kind.Type)
        node.addReferenceTo(typeNode, DocumentationReference.Kind.Detail)

        node.addReferenceTo(data, DocumentationReference.Kind.Owner)
        return node
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, when (descriptor.getKind()) {
            ClassKind.OBJECT -> DocumentationNode.Kind.Object
            ClassKind.TRAIT -> DocumentationNode.Kind.Trait
            else -> DocumentationNode.Kind.Class
        })
        data!!.addReferenceTo(node, DocumentationReference.Kind.Member)
        node.addReferenceTo(data, DocumentationReference.Kind.Owner)
        return node
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.Function)
        data!!.addReferenceTo(node, DocumentationReference.Kind.Member)

        val typeNode = DocumentationNode(descriptor, descriptor.getReturnType().toString(), DocumentationContent.Empty, DocumentationNode.Kind.Type)
        node.addReferenceTo(typeNode, DocumentationReference.Kind.Detail)

        node.addReferenceTo(data, DocumentationReference.Kind.Owner)
        return node
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.TypeParameter)
        data!!.addReferenceTo(node, DocumentationReference.Kind.Detail)
        val builtIns = KotlinBuiltIns.getInstance()
        for (constraint in descriptor.getUpperBounds()) {
            if (constraint == builtIns.getDefaultBound())
                continue
            val constraintNode = DocumentationNode(descriptor, constraint.toString(), DocumentationContent.Empty, DocumentationNode.Kind.UpperBound)
            node.addReferenceTo(constraintNode, DocumentationReference.Kind.Detail)
        }
        for (constraint in descriptor.getLowerBounds()) {
            if (builtIns.isNothing(constraint))
                continue
            val constraintNode = DocumentationNode(descriptor, constraint.toString(), DocumentationContent.Empty, DocumentationNode.Kind.LowerBound)
            node.addReferenceTo(constraintNode, DocumentationReference.Kind.Detail)
        }
        node.addReferenceTo(data, DocumentationReference.Kind.Owner)
        return node
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.Property)
        data!!.addReferenceTo(node, DocumentationReference.Kind.Member)

        val typeNode = DocumentationNode(descriptor, descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNode.Kind.Type)
        node.addReferenceTo(typeNode, DocumentationReference.Kind.Detail)

        node.addReferenceTo(data, DocumentationReference.Kind.Owner)
        return node
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.Constructor)
        data!!.addReferenceTo(node, DocumentationReference.Kind.Member)
        node.addReferenceTo(data, DocumentationReference.Kind.Owner)
        return node
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!, descriptor.fqName.asString(), DocumentationContent.Empty, DocumentationNode.Kind.Package)
        data!!.addReferenceTo(node, DocumentationReference.Kind.Member)
        node.addReferenceTo(data, DocumentationReference.Kind.Owner)
        return node
    }
}
