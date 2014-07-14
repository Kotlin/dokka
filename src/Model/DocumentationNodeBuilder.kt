package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.impl.*
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

class DocumentationNodeBuilder(val context: BindingContext) : DeclarationDescriptorVisitorEmptyBodies<DocumentationNode, DocumentationNode>() {

    fun reference(from: DocumentationNode, to: DocumentationNode, kind: DocumentationReference.Kind) {
        from.addReferenceTo(to, kind)
        if (kind == DocumentationReference.Kind.Link)
            to.addReferenceTo(from, DocumentationReference.Kind.Link)
        else
            to.addReferenceTo(from, DocumentationReference.Kind.Owner)
    }

    fun addModality(descriptor: MemberDescriptor, data: DocumentationNode): DocumentationNode {
        val modifier = descriptor.getModality().name().toLowerCase()
        val node = DocumentationNode(descriptor, modifier, DocumentationContent.Empty, DocumentationNode.Kind.Modifier)
        reference(data, node, DocumentationReference.Kind.Detail)
        return node
    }

    fun addVisibility(descriptor: MemberDescriptor, data: DocumentationNode): DocumentationNode {
        val modifier = descriptor.getVisibility().toString()
        val node = DocumentationNode(descriptor, modifier, DocumentationContent.Empty, DocumentationNode.Kind.Modifier)
        reference(data, node, DocumentationReference.Kind.Detail)
        return node
    }

    override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.Unknown)
        reference(data!!, node, DocumentationReference.Kind.Link)
        return node
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!, descriptor.getName().asString(), DocumentationContent.Empty, DocumentationNode.Kind.Receiver)
        reference(data!!, node, DocumentationReference.Kind.Detail)

        val typeNode = DocumentationNode(descriptor, descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNode.Kind.Type)
        reference(node, typeNode, DocumentationReference.Kind.Detail)

        return node
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.Parameter)
        reference(data!!, node, DocumentationReference.Kind.Detail)

        val typeNode = DocumentationNode(descriptor, descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNode.Kind.Type)
        reference(node, typeNode, DocumentationReference.Kind.Detail)

        return node
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, when (descriptor.getKind()) {
            ClassKind.OBJECT -> DocumentationNode.Kind.Object
            ClassKind.CLASS_OBJECT -> DocumentationNode.Kind.Object
            ClassKind.TRAIT -> DocumentationNode.Kind.Interface
            ClassKind.ENUM_CLASS -> DocumentationNode.Kind.Enum
            ClassKind.ENUM_ENTRY -> DocumentationNode.Kind.EnumItem
            else -> DocumentationNode.Kind.Class
        })
        reference(data!!, node, DocumentationReference.Kind.Member)
        addModality(descriptor, node)
        addVisibility(descriptor, node)
        return node
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.Function)
        reference(data!!, node, DocumentationReference.Kind.Member)

        val typeNode = DocumentationNode(descriptor, descriptor.getReturnType().toString(), DocumentationContent.Empty, DocumentationNode.Kind.Type)
        reference(node, typeNode, DocumentationReference.Kind.Detail)

        addModality(descriptor, node)
        addVisibility(descriptor, node)

        return node
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.TypeParameter)
        reference(data!!, node, DocumentationReference.Kind.Detail)
        val builtIns = KotlinBuiltIns.getInstance()
        for (constraint in descriptor.getUpperBounds()) {
            if (constraint == builtIns.getDefaultBound())
                continue
            val constraintNode = DocumentationNode(descriptor, constraint.toString(), DocumentationContent.Empty, DocumentationNode.Kind.UpperBound)
            reference(node, constraintNode, DocumentationReference.Kind.Detail)
        }
        for (constraint in descriptor.getLowerBounds()) {
            if (builtIns.isNothing(constraint))
                continue
            val constraintNode = DocumentationNode(descriptor, constraint.toString(), DocumentationContent.Empty, DocumentationNode.Kind.LowerBound)
            reference(node, constraintNode, DocumentationReference.Kind.Detail)
        }
        return node
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.Property)
        reference(data!!, node, DocumentationReference.Kind.Member)

        val typeNode = DocumentationNode(descriptor, descriptor.getType().toString(), DocumentationContent.Empty, DocumentationNode.Kind.Type)
        reference(node, typeNode, DocumentationReference.Kind.Detail)

        addModality(descriptor, node)
        addVisibility(descriptor, node)
        return node
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val doc = context.getDocumentation(descriptor!!)
        val node = DocumentationNode(descriptor, descriptor.getName().asString(), doc, DocumentationNode.Kind.Constructor)
        reference(data!!, node, DocumentationReference.Kind.Member)

        addVisibility(descriptor, node)

        return node
    }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!, descriptor.getFqName().asString(), DocumentationContent.Empty, DocumentationNode.Kind.Package)
        reference(data!!, node, DocumentationReference.Kind.Member)
        return node
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = DocumentationNode(descriptor!!, descriptor.fqName.asString(), DocumentationContent.Empty, DocumentationNode.Kind.Package)
        reference(data!!, node, DocumentationReference.Kind.Member)
        return node
    }
}
