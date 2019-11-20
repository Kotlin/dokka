package org.jetbrains.dokka

import org.jetbrains.dokka.Model.*
import org.jetbrains.dokka.Model.Function
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class DokkaDescriptorVisitor(
    val platformData: PlatformData,
    private val resolutionFacade: DokkaResolutionFacade
) : DeclarationDescriptorVisitorEmptyBodies<DocumentationNode<*>, DRI>() {
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
            descriptor.takeIf { it.isExpect }?.resolveDescriptorData(),
            listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData()),
            getXMLDRIs(listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData())).toMutableSet()
        )
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, parent: DRI): Property {
        val dri = parent.copy(callable = Callable.from(descriptor))
        return Property(
            dri,
            descriptor.name.asString(),
            descriptor.extensionReceiverParameter?.let { visitReceiverParameterDescriptor(it, dri) },
            descriptor.takeIf { it.isExpect }?.resolveDescriptorData(),
            listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData())
        )
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, parent: DRI): Function {
        val dri = parent.copy(callable = Callable.from(descriptor))
        return Function(
            dri,
            descriptor.name.asString(),
            descriptor.extensionReceiverParameter?.let { visitReceiverParameterDescriptor(it, dri) },
            descriptor.valueParameters.mapIndexed { index, desc -> parameter(index, desc, dri) },
            descriptor.takeIf { it.isExpect }?.resolveDescriptorData(),
            listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData())
        )
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, parent: DRI): Function {
        val dri = parent.copy(callable = Callable.from(descriptor))
        return Function(
            dri,
            "<init>",
            null,
            descriptor.valueParameters.mapIndexed { index, desc -> parameter(index, desc, dri) },
            descriptor.takeIf { it.isExpect }?.resolveDescriptorData(),
            listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData())
        )
    }

    override fun visitReceiverParameterDescriptor(
        descriptor: ReceiverParameterDescriptor,
        parent: DRI
    ) = Parameter(
        parent.copy(target = 0),
        null,
        listOf(descriptor.resolveDescriptorData())
    )

    private fun parameter(index: Int, descriptor: ValueParameterDescriptor, parent: DRI) =
        Parameter(
            parent.copy(target = index + 1),
            descriptor.name.asString(),
            listOf(descriptor.resolveDescriptorData())
        )

    private val FunctionDescriptor.isSynthetic: Boolean
        get() = (kind == FAKE_OVERRIDE || kind == SYNTHESIZED) && findKDoc() == null

    private fun MemberScope.functions(parent: DRI): List<Function> =
        getContributedDescriptors(DescriptorKindFilter.FUNCTIONS) { true }
            .filterIsInstance<FunctionDescriptor>()
            .filterNot { it.isSynthetic }
            .map { visitFunctionDescriptor(it, parent) }

    private fun MemberScope.properties(parent: DRI): List<Property> =
        getContributedDescriptors(DescriptorKindFilter.VALUES) { true }
            .filterIsInstance<PropertyDescriptor>()
            .map { visitPropertyDescriptor(it, parent) }

    private fun MemberScope.classes(parent: DRI): List<Class> =
        getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) { true }
            .filterIsInstance<ClassDescriptor>()
            .map { visitClassDescriptor(it, parent) }

    private fun <T : DeclarationDescriptor> T.resolveDescriptorData(): Descriptor<T> {
        val doc = findKDoc()
        val links = doc?.children?.filter { it is KDocLink }?.flatMap { link ->
            val destination = link.children.first { it is KDocName }.text
            resolveKDocLink(
                resolutionFacade.resolveSession.bindingContext,
                resolutionFacade,
                this,
                null,
                destination.split('.')
            ).map { Pair(destination, DRI.from(it)) }
        }?.toMap() ?: emptyMap()
        return Descriptor(this, doc, links, listOf(platformData))
    }

    private fun getXMLDRIs(descriptors: List<Descriptor<*>>) =
        descriptors.flatMap {
            it.docTag?.children
                ?.filter {
                    it.text.contains("@attr")
                }.orEmpty()
        }.flatMap { ref ->
            val matchResult = "@attr\\s+ref\\s+(.+)".toRegex().matchEntire(ref.text)
            val toFind = matchResult?.groups?.last()?.value.orEmpty()
            resolveKDocLink(
                resolutionFacade.resolveSession.bindingContext,
                resolutionFacade,
                descriptors.first().descriptor,
                null,
                toFind.split('.')
            ).map { DefaultExtra("@attr ref", DRI.from(it).toString()) }
        }
}

data class DefaultExtra(val key: String, val value: String) : Extra
