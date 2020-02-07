package org.jetbrains.dokka.transformers.descriptors

import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.ClassKind
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.Property
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.parsers.MarkdownParser
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import kotlin.reflect.KClass

object DefaultDescriptorToDocumentationTranslator : DescriptorToDocumentationTranslator {
    override fun invoke(
        moduleName: String,
        packageFragments: Iterable<PackageFragmentDescriptor>,
        platformData: PlatformData,
        context: DokkaContext
    ) = DokkaDescriptorVisitor(platformData, context.platforms[platformData]?.facade!!).run {
        packageFragments.map { visitPackageFragmentDescriptor(it, DRI.topLevel.withEmptyInfo()) }
    }.let { Module(moduleName, it) }

}


data class DRIWithPlatformInfo(
    val dri: DRI,
    val expected: PlatformInfo?,
    val actual: List<PlatformInfo>
)

fun DRI.withEmptyInfo() = DRIWithPlatformInfo(this, null, emptyList())

open class DokkaDescriptorVisitor(
    private val platformData: PlatformData,
    private val resolutionFacade: DokkaResolutionFacade
) : DeclarationDescriptorVisitorEmptyBodies<Documentable, DRIWithPlatformInfo>() {
    override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor, parent: DRIWithPlatformInfo): Nothing {
        throw IllegalStateException("${javaClass.simpleName} should never enter ${descriptor.javaClass.simpleName}")
    }

    override fun visitPackageFragmentDescriptor(
        descriptor: PackageFragmentDescriptor,
        parent: DRIWithPlatformInfo
    ): Package {
        val driWithPlatform = DRI(packageName = descriptor.fqName.asString()).withEmptyInfo()
        val scope = descriptor.getMemberScope()

        return Package(
            dri = driWithPlatform.dri,
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform)
        )
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): Classlike =
        when (descriptor.kind) {
            org.jetbrains.kotlin.descriptors.ClassKind.ENUM_CLASS -> enumDescriptor(descriptor, parent)
            else -> classDescriptor(descriptor, parent)
        }

    fun enumDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): Enum {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.getMemberScope(emptyList())
        val descriptorData = descriptor.takeUnless { it.isExpect }?.resolveClassDescriptionData()

        return Enum(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            entries = scope.classlikes(driWithPlatform).filter { it.kind == KotlinClassKindTypes.ENUM_ENTRY }.map {
                EnumEntry(
                    it
                )
            },
            constructors = descriptor.constructors.map { visitConstructorDescriptor(it, driWithPlatform) },
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            expected = descriptor.takeIf { it.isExpect }?.resolveClassDescriptionData(),
            actual = listOfNotNull(descriptorData),
            extra = mutableSetOf(), // TODO Implement following method to return proper results getXMLDRIs(descriptor, descriptorData).toMutableSet()
            visibility = mapOf(platformData to descriptor.visibility)
        )
    }

    fun classDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): Class {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.getMemberScope(emptyList())
        val descriptorData = descriptor.takeUnless { it.isExpect }?.resolveClassDescriptionData()
        val expected = descriptor.takeIf { it.isExpect }?.resolveClassDescriptionData()
        val actual = listOfNotNull(descriptorData)
        return Class(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            kind = KotlinClassKindTypes.valueOf(descriptor.kind.toString()),
            constructors = descriptor.constructors.map {
                visitConstructorDescriptor(
                    it,
                    if (it.isPrimary)
                        DRIWithPlatformInfo(
                            driWithPlatform.dri,
                            expected?.info.filterTagWrappers(listOf(Constructor::class)),
                            actual.filterTagWrappers(listOf(Constructor::class))
                        )
                    else
                        DRIWithPlatformInfo(driWithPlatform.dri, null, emptyList())
                )
            },
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            expected = expected,
            actual = actual,
            extra = mutableSetOf(), // TODO Implement following method to return proper results getXMLDRIs(descriptor, descriptorData).toMutableSet()
            visibility = mapOf(platformData to descriptor.visibility)
        )
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, parent: DRIWithPlatformInfo): Property {
        val expected = descriptor.takeIf { it.isExpect }?.resolveDescriptorData()
        val actual = listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData())
        val dri = parent.dri.copy(callable = Callable.from(descriptor))

        return Property(
            dri = dri,
            name = descriptor.name.asString(),
            receiver = descriptor.extensionReceiverParameter?.let {
                visitReceiverParameterDescriptor(
                    it,
                    DRIWithPlatformInfo(
                        dri,
                        expected?.filterTagWrappers(listOf(Receiver::class)),
                        actual.filterTagWrappers(listOf(Receiver::class))
                    )
                )
            },
            expected = expected,
            actual = actual,
            accessors = descriptor.accessors.map { visitPropertyAccessorDescriptor(it, descriptor, dri) },
            visibility = mapOf(platformData to descriptor.visibility)
        )
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, parent: DRIWithPlatformInfo): Function {
        val expected = descriptor.takeIf { it.isExpect }?.resolveDescriptorData()
        val actual = listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData())
        val dri = parent.dri.copy(callable = Callable.from(descriptor))

        return Function(
            dri = dri,
            name = descriptor.name.asString(),
            returnType = descriptor.returnType?.let { KotlinTypeWrapper(it) },
            isConstructor = false,
            receiver = descriptor.extensionReceiverParameter?.let {
                visitReceiverParameterDescriptor(
                    it,
                    DRIWithPlatformInfo(
                        dri,
                        expected?.filterTagWrappers(listOf(Receiver::class)),
                        actual.filterTagWrappers(listOf(Receiver::class))
                    )
                )
            },
            parameters = descriptor.valueParameters.mapIndexed { index, desc ->
                parameter(
                    index, desc,
                    DRIWithPlatformInfo(
                        dri,
                        expected.filterTagWrappers(listOf(Param::class), desc.name.asString()),
                        actual.filterTagWrappers(listOf(Param::class), desc.name.asString())
                    )
                )
            },
            expected = expected,
            actual = actual,
            visibility = mapOf(platformData to descriptor.visibility)
        )
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, parent: DRIWithPlatformInfo): Function {
        val dri = parent.dri.copy(callable = Callable.from(descriptor))
        return Function(
            dri = dri,
            name = "<init>",
            returnType = KotlinTypeWrapper(descriptor.returnType),
            isConstructor = true,
            receiver = null,
            parameters = descriptor.valueParameters.mapIndexed { index, desc ->
                parameter(
                    index, desc,
                    DRIWithPlatformInfo(
                        dri,
                        parent.expected.filterTagWrappers(listOf(Param::class)),
                        parent.actual.filterTagWrappers(listOf(Param::class))
                    )
                )
            },
            expected = parent.expected ?: descriptor.takeIf { it.isExpect }?.resolveDescriptorData(),
            actual = parent.actual,
            visibility = mapOf(platformData to descriptor.visibility)
        )
    }

    override fun visitReceiverParameterDescriptor(
        descriptor: ReceiverParameterDescriptor,
        parent: DRIWithPlatformInfo
    ) = Parameter(
        dri = parent.dri.copy(target = 0),
        name = null,
        type = KotlinTypeWrapper(descriptor.type),
        expected = parent.expected,
        actual = parent.actual
    )

    open fun visitPropertyAccessorDescriptor(
        descriptor: PropertyAccessorDescriptor,
        propertyDescriptor: PropertyDescriptor,
        parent: DRI
    ): Function {
        val dri = parent.copy(callable = Callable.from(descriptor))
        val isGetter = descriptor is PropertyGetterDescriptor

        fun PropertyDescriptor.asParameter(parent: DRI) =
            Parameter(
                parent.copy(target = 1),
                this.name.asString(),
                KotlinTypeWrapper(this.type),
                descriptor.takeIf { it.isExpect }?.resolveDescriptorData(),
                listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData())
            )

        val name = run {
            val modifier = if (isGetter) "get" else "set"
            val rawName = propertyDescriptor.name.asString()
            "$modifier${rawName[0].toUpperCase()}${rawName.drop(1)}"
        }

        descriptor.visibility
        val parameters =
            if (isGetter) {
                emptyList()
            } else {
                listOf(propertyDescriptor.asParameter(dri))
            }

        return Function(
            dri,
            name,
            descriptor.returnType?.let { KotlinTypeWrapper(it) },
            false,
            null,
            parameters,
            descriptor.takeIf { it.isExpect }?.resolveDescriptorData(),
            listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData()),
            visibility = mapOf(platformData to descriptor.visibility)
        )
    }

    private fun parameter(index: Int, descriptor: ValueParameterDescriptor, parent: DRIWithPlatformInfo) =
        Parameter(
            dri = parent.dri.copy(target = index + 1),
            name = descriptor.name.asString(),
            type = KotlinTypeWrapper(descriptor.type),
            expected = parent.expected,
            actual = parent.actual,
            isExtension = descriptor.type.isExtensionFunctionType
        )

    private fun MemberScope.functions(parent: DRIWithPlatformInfo): List<Function> =
        getContributedDescriptors(DescriptorKindFilter.FUNCTIONS) { true }
            .filterIsInstance<FunctionDescriptor>()
            .map { visitFunctionDescriptor(it, parent) }

    private fun MemberScope.properties(parent: DRIWithPlatformInfo): List<Property> =
        getContributedDescriptors(DescriptorKindFilter.VALUES) { true }
            .filterIsInstance<PropertyDescriptor>()
            .map { visitPropertyDescriptor(it, parent) }

    private fun MemberScope.classlikes(parent: DRIWithPlatformInfo): List<Classlike> =
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

    private fun PlatformInfo?.filterTagWrappers(
        types: List<KClass<out TagWrapper>>,
        name: String? = null
    ): PlatformInfo? {
        if (this == null)
            return null
        return BasePlatformInfo(
            DocumentationNode(
                this.documentationNode.children.filter { it::class in types && (it as? NamedTagWrapper)?.name == name }
            ),
            this.platformData
        )
    }

    private fun List<PlatformInfo>.filterTagWrappers(
        types: List<KClass<out TagWrapper>>,
        name: String? = null
    ): List<PlatformInfo> =
        this.map { it.filterTagWrappers(types, name)!! }
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

class KotlinTypeWrapper(val kotlinType: KotlinType) : TypeWrapper {
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