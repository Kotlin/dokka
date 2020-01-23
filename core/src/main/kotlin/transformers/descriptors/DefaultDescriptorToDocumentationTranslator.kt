package org.jetbrains.dokka.transformers.descriptors

import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.ClassKind
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.parsers.MarkdownParser
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType

object DefaultDescriptorToDocumentationTranslator : DescriptorToDocumentationTranslator {
    override fun invoke(
        moduleName: String,
        packageFragments: Iterable<PackageFragmentDescriptor>,
        platformData: PlatformData,
        context: DokkaContext
    ) = DokkaDescriptorVisitor(platformData, context.platforms[platformData]?.facade!!).run {
        packageFragments.map { visitPackageFragmentDescriptor(it, DRI.topLevel) }
    }.let { Module(moduleName, it) }

}

class DokkaDescriptorVisitor(
    private val platformData: PlatformData,
    private val resolutionFacade: DokkaResolutionFacade
) : DeclarationDescriptorVisitorEmptyBodies<Documentable, DRI>() {
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
            scope.classlikes(dri)
        )
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, parent: DRI): Classlike = when (descriptor.kind) {
        org.jetbrains.kotlin.descriptors.ClassKind.ENUM_CLASS -> enumDescriptor(descriptor, parent)
        else -> classDescriptor(descriptor, parent)
    }

    fun enumDescriptor(descriptor: ClassDescriptor, parent: DRI): Enum {
        val dri = parent.withClass(descriptor.name.asString())
        val scope = descriptor.getMemberScope(emptyList())
        val descriptorData = descriptor.takeUnless { it.isExpect }?.resolveClassDescriptionData()

        return Enum(
            dri = dri,
            name = descriptor.name.asString(),
            entries = scope.classlikes(dri).filter { it.kind == KotlinClassKindTypes.ENUM_ENTRY }.map { EnumEntry(it) },
            constructors = descriptor.constructors.map { visitConstructorDescriptor(it, dri) },
            functions = scope.functions(dri),
            properties = scope.properties(dri),
            classlikes = scope.classlikes(dri),
            expected = descriptor.takeIf { it.isExpect }?.resolveClassDescriptionData(),
            actual = listOfNotNull(descriptorData),
            extra = mutableSetOf() // TODO Implement following method to return proper results getXMLDRIs(descriptor, descriptorData).toMutableSet()
        )
    }

    fun classDescriptor(descriptor: ClassDescriptor, parent: DRI): Class {
        val dri = parent.withClass(descriptor.name.asString())
        val scope = descriptor.getMemberScope(emptyList())
        val descriptorData = descriptor.takeUnless { it.isExpect }?.resolveClassDescriptionData()
        return Class(
            dri,
            descriptor.name.asString(),
            KotlinClassKindTypes.valueOf(descriptor.kind.toString()),
            descriptor.constructors.map { visitConstructorDescriptor(it, dri) },
            scope.functions(dri),
            scope.properties(dri),
            scope.classlikes(dri),
            descriptor.takeIf { it.isExpect }?.resolveClassDescriptionData(),
            listOfNotNull(descriptorData),
            mutableSetOf() // TODO Implement following method to return proper results getXMLDRIs(descriptor, descriptorData).toMutableSet()
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
            descriptor.returnType?.let { KotlinTypeWrapper(it) },
            false,
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
            KotlinTypeWrapper(descriptor.returnType),
            true,
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
        KotlinTypeWrapper(descriptor.type),
        listOf(descriptor.resolveDescriptorData())
    )

    private fun parameter(index: Int, descriptor: ValueParameterDescriptor, parent: DRI) =
        Parameter(
            parent.copy(target = index + 1),
            descriptor.name.asString(),
            KotlinTypeWrapper(descriptor.type),
            listOf(descriptor.resolveDescriptorData())
        )

    private fun MemberScope.functions(parent: DRI): List<Function> =
        getContributedDescriptors(DescriptorKindFilter.FUNCTIONS) { true }
            .filterIsInstance<FunctionDescriptor>()
            .map { visitFunctionDescriptor(it, parent) }

    private fun MemberScope.properties(parent: DRI): List<Property> =
        getContributedDescriptors(DescriptorKindFilter.VALUES) { true }
            .filterIsInstance<PropertyDescriptor>()
            .map { visitPropertyDescriptor(it, parent) }

    private fun MemberScope.classlikes(parent: DRI): List<Classlike> =
        getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) { true }
            .filterIsInstance<ClassDescriptor>()
            .map { visitClassDescriptor(it, parent) }

    private fun DeclarationDescriptor.resolveDescriptorData(): PlatformInfo {
        val doc = findKDoc()
        val parser: MarkdownParser = MarkdownParser(resolutionFacade, this)
        val docHeader = parser.parseFromKDocTag(doc)
        return BasePlatformInfo(docHeader, listOf(platformData))
    }

    private fun ClassDescriptor.resolveClassDescriptionData(): ClassPlatformInfo {
        return ClassPlatformInfo(resolveDescriptorData(),
            (getSuperInterfaces() + getAllSuperclassesWithoutAny()).map { DRI.from(it) })
    }
}

data class XMLMega(val key: String, val dri: DRI) : Extra

enum class KotlinClassKindTypes : ClassKind {
    CLASS,
    INTERFACE,
    ENUM_CLASS,
    ENUM_ENTRY,
    ANNOTATION_CLASS,
    OBJECT;
}

class KotlinTypeWrapper(private val kotlinType: KotlinType) : TypeWrapper {
    private val declarationDescriptor = kotlinType.constructor.declarationDescriptor
    private val fqNameSafe = declarationDescriptor?.fqNameSafe
    override val constructorFqName = fqNameSafe?.asString()
    override val constructorNamePathSegments: List<String> =
        fqNameSafe?.pathSegments()?.map { it.asString() } ?: emptyList()
    override val arguments: List<KotlinTypeWrapper> by lazy {
        kotlinType.arguments.map {
            KotlinTypeWrapper(
                it.type
            )
        }
    }
    override val dri: DRI? by lazy { declarationDescriptor?.let { DRI.from(it) } }
}