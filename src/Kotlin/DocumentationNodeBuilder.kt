package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.jet.lang.descriptors.MemberDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.Named
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

class DocumentationNodeBuilder(val context: DocumentationContext) : DeclarationDescriptorVisitorEmptyBodies<DocumentationNode, DocumentationNode>() {

    fun reference(from: DocumentationNode, to: DocumentationNode, kind: DocumentationReference.Kind) {
        from.addReferenceTo(to, kind)
        when (kind) {
            DocumentationReference.Kind.Detail -> to.addReferenceTo(from, DocumentationReference.Kind.Owner)
            DocumentationReference.Kind.Member -> to.addReferenceTo(from, DocumentationReference.Kind.Owner)
            DocumentationReference.Kind.Owner -> to.addReferenceTo(from, DocumentationReference.Kind.Member)
        }
    }

    fun addModality(descriptor: MemberDescriptor, data: DocumentationNode) {
        val modifier = descriptor.getModality().name().toLowerCase()
        val node = DocumentationNode(modifier, Content.Empty, DocumentationNode.Kind.Modifier)
        reference(data, node, DocumentationReference.Kind.Detail)
    }

    fun addVisibility(descriptor: MemberDescriptor, data: DocumentationNode) {
        val modifier = descriptor.getVisibility().toString()
        val node = DocumentationNode(modifier, Content.Empty, DocumentationNode.Kind.Modifier)
        reference(data, node, DocumentationReference.Kind.Detail)
    }

    fun addType(descriptor: DeclarationDescriptor, t: JetType?, data: DocumentationNode) {
        if (t == null)
            return
        val classifierDescriptor = t.getConstructor().getDeclarationDescriptor()
        val name = when (classifierDescriptor) {
            is Named -> classifierDescriptor.getName().asString()
            else -> "<anonymous>"
        }
        val node = DocumentationNode(name, Content.Empty, DocumentationNode.Kind.Type)
        if (classifierDescriptor != null)
            context.attach(node, classifierDescriptor)

        reference(data, node, DocumentationReference.Kind.Detail)
        for (param in t.getArguments())
            addType(descriptor, param.getType(), node)
    }

    override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor?, data: DocumentationNode?): DocumentationNode? {
        descriptor!!
        val doc = context.parseDocumentation(descriptor)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNode.Kind.Unknown)
        reference(data!!, node, DocumentationReference.Kind.Member)
        context.register(descriptor, node)
        return node
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        descriptor!!
        val node = DocumentationNode(descriptor.getName().asString(), Content.Empty, DocumentationNode.Kind.Receiver)
        reference(data!!, node, DocumentationReference.Kind.Detail)

        addType(descriptor, descriptor.getType(), node)

        return node
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        descriptor!!
        val doc = context.parseDocumentation(descriptor)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNode.Kind.Parameter)
        reference(data!!, node, DocumentationReference.Kind.Detail)

        addType(descriptor, descriptor.getType(), node)

        return node
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor?, data: DocumentationNode?): DocumentationNode? {
        descriptor!!
        val doc = context.parseDocumentation(descriptor)
        val node = DocumentationNode(descriptor.getName().asString(), doc, when (descriptor.getKind()) {
            ClassKind.OBJECT -> org.jetbrains.dokka.DocumentationNode.Kind.Object
            ClassKind.CLASS_OBJECT -> org.jetbrains.dokka.DocumentationNode.Kind.Object
            ClassKind.TRAIT -> org.jetbrains.dokka.DocumentationNode.Kind.Interface
            ClassKind.ENUM_CLASS -> org.jetbrains.dokka.DocumentationNode.Kind.Enum
            ClassKind.ENUM_ENTRY -> org.jetbrains.dokka.DocumentationNode.Kind.EnumItem
            else -> DocumentationNode.Kind.Class
        })
        reference(data!!, node, DocumentationReference.Kind.Member)
        addModality(descriptor, node)
        addVisibility(descriptor, node)
        context.register(descriptor, node)
        return node
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor?, data: DocumentationNode?): DocumentationNode? {
        descriptor!!
        val doc = context.parseDocumentation(descriptor)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNode.Kind.Function)
        reference(data!!, node, DocumentationReference.Kind.Member)

        addType(descriptor, descriptor.getReturnType(), node)
        addModality(descriptor, node)
        addVisibility(descriptor, node)
        context.register(descriptor, node)
        return node
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        descriptor!!
        val doc = context.parseDocumentation(descriptor)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNode.Kind.TypeParameter)
        reference(data!!, node, DocumentationReference.Kind.Detail)
        val builtIns = KotlinBuiltIns.getInstance()
        for (constraint in descriptor.getUpperBounds()) {
            if (constraint == builtIns.getDefaultBound())
                continue
            val constraintNode = DocumentationNode(constraint.toString(), Content.Empty, DocumentationNode.Kind.UpperBound)
            reference(node, constraintNode, DocumentationReference.Kind.Detail)
        }
        for (constraint in descriptor.getLowerBounds()) {
            if (builtIns.isNothing(constraint))
                continue
            val constraintNode = DocumentationNode(constraint.toString(), Content.Empty, DocumentationNode.Kind.LowerBound)
            reference(node, constraintNode, DocumentationReference.Kind.Detail)
        }
        return node
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor?, data: DocumentationNode?): DocumentationNode? {
        descriptor!!
        val doc = context.parseDocumentation(descriptor)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNode.Kind.Property)
        reference(data!!, node, DocumentationReference.Kind.Member)

        addType(descriptor, descriptor.getType(), node)
        addModality(descriptor, node)
        addVisibility(descriptor, node)
        context.register(descriptor, node)
        return node
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor?, data: DocumentationNode?): DocumentationNode? {
        descriptor!!
        val doc = context.parseDocumentation(descriptor)
        val node = DocumentationNode(descriptor.getName().asString(), doc, DocumentationNode.Kind.Constructor)
        reference(data!!, node, DocumentationReference.Kind.Member)

        addVisibility(descriptor, node)
        context.register(descriptor, node)
        return node
    }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor?, data: DocumentationNode?): DocumentationNode? {
        descriptor!!
        val node = DocumentationNode(descriptor.getFqName().asString(), Content.Empty, DocumentationNode.Kind.Package)
        reference(data!!, node, DocumentationReference.Kind.Member)
        return node
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor?, data: DocumentationNode?): DocumentationNode? {
        descriptor!!
        val node = DocumentationNode(descriptor.fqName.asString(), Content.Empty, DocumentationNode.Kind.Package)
        reference(data!!, node, DocumentationReference.Kind.Member)
        return node
    }
}