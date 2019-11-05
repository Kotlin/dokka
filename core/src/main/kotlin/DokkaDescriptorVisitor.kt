package org.jetbrains.dokka

import org.jetbrains.dokka.Model.*
import org.jetbrains.dokka.Model.Function
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.idea.kdoc.findKDoc

object DokkaDescriptorVisitor : DeclarationDescriptorVisitorEmptyBodies<DocumentationNode<*>, DRI>() {
    override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor, parent: DRI): Nothing {
        throw IllegalStateException("${javaClass.simpleName} should never enter ${descriptor.javaClass.simpleName}")
    }

    override fun visitPackageFragmentDescriptor(
        descriptor: PackageFragmentDescriptor,
        parent: DRI
    ): Package {
        val dri = DRI(packageName = descriptor.fqName.asString())
        val scope = descriptor.getMemberScope()
        return Package(
            dri,
            scope.functions(dri),
            scope.properties(dri),
            scope.classes(dri)
        )
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, parent: DRI): Class {
        val dri = parent.withClass(descriptor.name.asString())
        val scope = descriptor.getMemberScope(emptyList())
        return Class(
            dri,
            descriptor.name.asString(),
            descriptor.constructors.map { visitConstructorDescriptor(it, dri) },
            scope.functions(dri),
            scope.properties(dri),
            scope.classes(dri),
            listOfNotNull(descriptor.findKDoc()),
            listOf(descriptor)
        )
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, parent: DRI): Property {
        val dri = parent.copy(callable = Callable.from(descriptor))
        return Property(
            dri,
            descriptor.name.asString(),
            descriptor.extensionReceiverParameter?.let { visitReceiverParameterDescriptor(it, dri) },
            listOfNotNull(descriptor.findKDoc()),
            listOf(descriptor)
        )
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, parent: DRI): Function {
        val dri = parent.copy(callable = Callable.from(descriptor))
        return Function(
            dri,
            descriptor.name.asString(),
            descriptor.extensionReceiverParameter?.let { visitReceiverParameterDescriptor(it, dri) },
            descriptor.valueParameters.mapIndexed { index, desc -> parameter(index, desc, dri) },
            listOfNotNull(descriptor.findKDoc()),
            listOf(descriptor)
        )
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, parent: DRI): Function {
        val dri = parent.copy(callable = Callable.from(descriptor))
        return Function(
            dri,
            "<init>",
            null,
            descriptor.valueParameters.mapIndexed { index, desc -> parameter(index, desc, dri) },
            listOfNotNull(descriptor.findKDoc()),
            listOf(descriptor)
        )
    }

    override fun visitReceiverParameterDescriptor(
        descriptor: ReceiverParameterDescriptor,
        parent: DRI
    ) = Parameter(
        parent.copy(target = 0),
        null,
        listOfNotNull(descriptor.findKDoc()),
        listOf(descriptor)
    )

    private fun parameter(index: Int, descriptor: ValueParameterDescriptor, parent: DRI) =
        Parameter(
            parent.copy(target = index + 1),
            descriptor.name.asString(),
            listOfNotNull(descriptor.findKDoc()),
            listOf(descriptor)
        )

    private fun MemberScope.functions(parent: DRI): List<Function> =
        getContributedDescriptors(DescriptorKindFilter.FUNCTIONS) { true }
            .filterIsInstance<FunctionDescriptor>()
            .map { visitFunctionDescriptor(it, parent) }

    private fun MemberScope.properties(parent: DRI): List<Property> =
        getContributedDescriptors(DescriptorKindFilter.VALUES) { true }
            .filterIsInstance<PropertyDescriptor>()
            .map { visitPropertyDescriptor(it, parent) }

    private fun MemberScope.classes(parent: DRI): List<Class> =
        getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) { true }
            .filterIsInstance<ClassDescriptor>()
            .map { visitClassDescriptor(it, parent) }
}
