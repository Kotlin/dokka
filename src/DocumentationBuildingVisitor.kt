package com.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.name.FqName

class DocumentationBuildingVisitor(private val worker: DeclarationDescriptorVisitor<DocumentationNode, DocumentationNode>)
: DeclarationDescriptorVisitor<DocumentationNode, DocumentationNode> {

    private fun visitChildren(descriptors: Collection<DeclarationDescriptor>, data: DocumentationNode) {
        for (descriptor in descriptors) {
            descriptor.accept(this, data)
        }
    }

    private fun visitChildren(descriptor: DeclarationDescriptor?, data: DocumentationNode) {
        descriptor?.accept(this, data)
    }

    private fun createDocumentation(descriptor: DeclarationDescriptor, data: DocumentationNode): DocumentationNode {
        return descriptor.accept(worker, data)
    }

    private fun processCallable(descriptor: CallableDescriptor, data: DocumentationNode): DocumentationNode {
        val node = createDocumentation(descriptor, data)
        visitChildren(descriptor.getTypeParameters(), node)
        visitChildren(descriptor.getReceiverParameter(), node)
        visitChildren(descriptor.getValueParameters(), node)
        return node
    }

    public override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = createDocumentation(descriptor!!, data!!)
        visitChildren(descriptor.getMemberScope().getAllDescriptors(), node)
        return node
    }

    public override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = createDocumentation(descriptor!!, data!!)
        visitChildren(descriptor.getMemberScope().getAllDescriptors(), node)
        return node
    }

    public override fun visitVariableDescriptor(descriptor: VariableDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = processCallable(descriptor!!, data!!)
        return node
    }

    public override fun visitPropertyDescriptor(descriptor: PropertyDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = processCallable(descriptor!!, data!!)
        visitChildren(descriptor.getGetter(), node)
        visitChildren(descriptor.getSetter(), node)
        return node
    }

    public override fun visitFunctionDescriptor(descriptor: FunctionDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = processCallable(descriptor!!, data!!)
        return node
    }

    public override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = createDocumentation(descriptor!!, data!!)
        return node
    }

    public override fun visitClassDescriptor(descriptor: ClassDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = createDocumentation(descriptor!!, data!!)
        visitChildren(descriptor.getConstructors(), node)
        visitChildren(descriptor.getTypeConstructor().getParameters(), node)
        visitChildren(descriptor.getClassObjectDescriptor(), node)
        val members = descriptor.getDefaultType().getMemberScope().getAllDescriptors().filter {
            it !is CallableMemberDescriptor || it.getKind().isReal()
        }
        visitChildren(members, node)
        return node
    }

    public override fun visitModuleDeclaration(descriptor: ModuleDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = createDocumentation(descriptor!!, data!!)
        visitChildren(descriptor.getPackage(FqName.ROOT), node)
        return node
    }

    public override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = visitFunctionDescriptor(constructorDescriptor, data)
        return node
    }

    public override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = visitClassDescriptor(scriptDescriptor!!.getClassDescriptor(), data)
        return node
    }

    public override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = visitVariableDescriptor(descriptor, data)
        return node
    }

    public override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = visitFunctionDescriptor(descriptor, data)
        return node
    }

    public override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = visitFunctionDescriptor(descriptor, data)
        return node
    }

    public override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor?, data: DocumentationNode?): DocumentationNode? {
        val node = createDocumentation(descriptor!!, data!!)
        return node
    }
}

public fun visitDescriptor(descriptor: DeclarationDescriptor, data: DocumentationNode, visitor: DeclarationDescriptorVisitor<DocumentationNode, DocumentationNode>): DocumentationNode {
    return descriptor.accept(DocumentationBuildingVisitor(visitor), data)
}
