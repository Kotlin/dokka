package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.parsers.MarkdownParser
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.descriptors.DescriptorToDocumentableTranslator
import org.jetbrains.kotlin.codegen.isJvmStaticInObjectOrClassOrInterface
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.dokka.model.Variance
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf.fqName
import org.jetbrains.kotlin.idea.kdoc.findKDoc

class DefaultDescriptorToDocumentableTranslator(
    private val context: DokkaContext
) : DescriptorToDocumentableTranslator {
    override fun invoke(
        moduleName: String,
        packageFragments: Iterable<PackageFragmentDescriptor>,
        platformData: PlatformData
    ) = DokkaDescriptorVisitor(platformData, context.platforms.getValue(platformData).facade).run {
        packageFragments.map {
            visitPackageFragmentDescriptor(
                it,
                DRIWithPlatformInfo(DRI.topLevel, PlatformDependent.empty())
            )
        }
    }.let { Module(moduleName, it, PlatformDependent.empty(), listOf(platformData)) }

}

data class DRIWithPlatformInfo(
    val dri: DRI,
    val actual: PlatformDependent<DocumentableSource>
)

fun DRI.withEmptyInfo() = DRIWithPlatformInfo(this, PlatformDependent.empty())

private class DokkaDescriptorVisitor( // TODO: close this class and make it private together with DRIWithPlatformInfo
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
            classlikes = scope.classlikes(driWithPlatform),
            packages = scope.packages(driWithPlatform),
            documentation = descriptor.resolveDescriptorData(platformData),
            platformData = listOf(platformData)
        )
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): Classlike =
        when (descriptor.kind) {
            ClassKind.ENUM_CLASS -> enumDescriptor(descriptor, parent)
            ClassKind.ENUM_ENTRY -> enumDescriptor(descriptor, parent)
            ClassKind.OBJECT -> objectDescriptor(descriptor, parent)
            ClassKind.INTERFACE -> interfaceDescriptor(descriptor, parent)
            else -> classDescriptor(descriptor, parent)
        }

    private fun interfaceDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): Interface {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val info = descriptor.resolveClassDescriptionData(platformData)

        return Interface(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            sources = descriptor.createSources(),
            visibility = PlatformDependent.from(platformData, descriptor.visibility.toDokkaVisibility()),
            supertypes = PlatformDependent.from(platformData, info.supertypes),
            documentation = info.docs,
            generics = descriptor.typeConstructor.parameters.map { it.toTypeParameter() },
            companion = descriptor.companion(driWithPlatform),
            platformData = listOf(platformData),
            extra = descriptor.additionalExtras()
        )
    }

    private fun objectDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): Object {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val info = descriptor.resolveClassDescriptionData(platformData)

        return Object(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            sources = descriptor.createSources(),
            visibility = PlatformDependent(mapOf(platformData to descriptor.visibility.toDokkaVisibility())),
            supertypes = PlatformDependent.from(platformData, info.supertypes),
            documentation = info.docs,
            platformData = listOf(platformData),
            extra = descriptor.additionalExtras()
        )
    }

    private fun enumDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): Enum {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val info = descriptor.resolveClassDescriptionData(platformData)

        return Enum(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            entries = scope.enumEntries(driWithPlatform),
            constructors = descriptor.constructors.map { visitConstructorDescriptor(it, driWithPlatform) },
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            sources = descriptor.createSources(),
            visibility = PlatformDependent(mapOf(platformData to descriptor.visibility.toDokkaVisibility())),
            supertypes = PlatformDependent.from(platformData, info.supertypes),
            documentation = info.docs,
            companion = descriptor.companion(driWithPlatform),
            platformData = listOf(platformData),
            extra = descriptor.additionalExtras()
        )
    }

    private fun enumEntryDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): EnumEntry {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope

        return EnumEntry(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            documentation = descriptor.resolveDescriptorData(platformData),
            classlikes = scope.classlikes(driWithPlatform),
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            platformData = listOf(platformData),
            extra = descriptor.additionalExtras()
        )
    }

    private fun classDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): Class {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val info = descriptor.resolveClassDescriptionData(platformData)
        val actual = descriptor.createSources()

        return Class(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            constructors = descriptor.constructors.map {
                visitConstructorDescriptor(
                    it,
                    if (it.isPrimary) DRIWithPlatformInfo(driWithPlatform.dri, actual)
                    else DRIWithPlatformInfo(driWithPlatform.dri, PlatformDependent.empty())
                )
            },
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            sources = actual,
            visibility = PlatformDependent.from(platformData, descriptor.visibility.toDokkaVisibility()),
            generics = descriptor.typeConstructor.parameters.map { it.toTypeParameter() },
            documentation = info.docs,
            modifier = descriptor.modifier(),
            companion = descriptor.companion(driWithPlatform),
            supertypes = PlatformDependent.from(platformData, info.supertypes),
            platformData = listOf(platformData),
            extra = descriptor.additionalExtras()
        )
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, parent: DRIWithPlatformInfo): Property {
        val dri = parent.dri.copy(callable = Callable.from(descriptor))

        val actual = descriptor.createSources()
        return Property(
            dri = dri,
            name = descriptor.name.asString(),
            receiver = descriptor.extensionReceiverParameter?.let {
                visitReceiverParameterDescriptor(it, DRIWithPlatformInfo(dri, actual))
            },
            sources = actual,
            getter = descriptor.accessors.filterIsInstance<PropertyGetterDescriptor>().singleOrNull()?.let {
               visitPropertyAccessorDescriptor(it, descriptor, dri)
            },
            setter = descriptor.accessors.filterIsInstance<PropertySetterDescriptor>().singleOrNull()?.let {
                visitPropertyAccessorDescriptor(it, descriptor, dri)
            },
            visibility = PlatformDependent(mapOf(platformData to descriptor.visibility.toDokkaVisibility())),
            documentation = descriptor.resolveDescriptorData(platformData),
            modifier = descriptor.modifier(),
            type = KotlinTypeWrapper(descriptor.returnType!!),
            platformData = listOf(platformData),
            extra = descriptor.additionalExtras()
        )
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, parent: DRIWithPlatformInfo): Function {
        val dri = parent.dri.copy(callable = Callable.from(descriptor))

        val actual = descriptor.createSources()
        return Function(
            dri = dri,
            name = descriptor.name.asString(),
            isConstructor = false,
            receiver = descriptor.extensionReceiverParameter?.let {
                visitReceiverParameterDescriptor(it, DRIWithPlatformInfo(dri, actual))
            },
            parameters = descriptor.valueParameters.mapIndexed { index, desc ->
                parameter(index, desc, DRIWithPlatformInfo(dri, actual))
            },
            sources = actual,
            visibility = PlatformDependent.from(platformData, descriptor.visibility.toDokkaVisibility()),
            generics = descriptor.typeParameters.map { it.toTypeParameter() },
            documentation = descriptor.resolveDescriptorData(platformData),
            modifier = descriptor.modifier(),
            type = KotlinTypeWrapper(descriptor.returnType!!),
            platformData = listOf(platformData),
            extra = descriptor.additionalExtras()
        )
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, parent: DRIWithPlatformInfo): Function {
        val dri = parent.dri.copy(callable = Callable.from(descriptor))
        val actual = descriptor.createSources()
        return Function(
            dri = dri,
            name = "<init>",
            isConstructor = true,
            receiver = descriptor.extensionReceiverParameter?.let {
                visitReceiverParameterDescriptor(it, DRIWithPlatformInfo(dri, actual))
            },
            parameters = descriptor.valueParameters.mapIndexed { index, desc ->
                parameter(index, desc, DRIWithPlatformInfo(dri, actual))
            },
            sources = actual,
            visibility = PlatformDependent(mapOf(platformData to descriptor.visibility.toDokkaVisibility())),
            documentation = descriptor.resolveDescriptorData(platformData),
            type = KotlinTypeWrapper(descriptor.returnType),
            modifier = descriptor.modifier(),
            generics = descriptor.typeParameters.map { it.toTypeParameter() },
            platformData = listOf(platformData),
            extra = descriptor.additionalExtras()
        )
    }

    override fun visitReceiverParameterDescriptor(
        descriptor: ReceiverParameterDescriptor,
        parent: DRIWithPlatformInfo
    ) = Parameter(
        dri = parent.dri.copy(target = 0),
        name = null,
        type = KotlinTypeWrapper(descriptor.type),
        documentation = descriptor.resolveDescriptorData(platformData),
        platformData = listOf(platformData)
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
                type = KotlinTypeWrapper(this.type),
                documentation = descriptor.resolveDescriptorData(platformData),
                platformData = listOf(platformData),
                extra = descriptor.additionalExtras()
            )

        val name = run {
            val modifier = if (isGetter) "get" else "set"
            val rawName = propertyDescriptor.name.asString()
            "$modifier${rawName[0].toUpperCase()}${rawName.drop(1)}"
        }

        val parameters =
            if (isGetter) {
                emptyList()
            } else {
                listOf(propertyDescriptor.asParameter(dri))
            }

        return Function(
            dri,
            name,
            isConstructor = false,
            parameters = parameters,
            visibility = PlatformDependent(mapOf(platformData to descriptor.visibility.toDokkaVisibility())),
            documentation = descriptor.resolveDescriptorData(platformData),
            type = KotlinTypeWrapper(descriptor.returnType!!),
            generics = descriptor.typeParameters.map { it.toTypeParameter() },
            modifier = descriptor.modifier(),
            receiver = descriptor.extensionReceiverParameter?.let {
                visitReceiverParameterDescriptor(
                    it,
                    DRIWithPlatformInfo(dri, descriptor.createSources())
                )
            },
            sources = descriptor.createSources(),
            platformData = listOf(platformData),
            extra = descriptor.additionalExtras()
        )
    }

    private fun parameter(index: Int, descriptor: ValueParameterDescriptor, parent: DRIWithPlatformInfo) =
        Parameter(
            dri = parent.dri.copy(target = index + 1),
            name = descriptor.name.asString(),
            type = KotlinTypeWrapper(descriptor.type),
            documentation = descriptor.resolveDescriptorData(platformData),
            platformData = listOf(platformData),
            extra = descriptor.additionalExtras()
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
            .mapNotNull { it as? Classlike }

    private fun MemberScope.packages(parent: DRIWithPlatformInfo): List<Package> =
        getContributedDescriptors(DescriptorKindFilter.PACKAGES) { true }
            .filterIsInstance<PackageFragmentDescriptor>()
            .map { visitPackageFragmentDescriptor(it, parent) }

    private fun MemberScope.enumEntries(parent: DRIWithPlatformInfo): List<EnumEntry> =
        this.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) { true }
            .filterIsInstance<ClassDescriptor>()
            .map { enumEntryDescriptor(it, parent) }


    private fun DeclarationDescriptor.resolveDescriptorData(platformData: PlatformData): PlatformDependent<DocumentationNode> =
        PlatformDependent.from(platformData, getDocumentation())

    private fun ClassDescriptor.resolveClassDescriptionData(platformData: PlatformData): ClassInfo {
        return ClassInfo(
            (getSuperInterfaces() + getAllSuperclassesWithoutAny()).map { DRI.from(it) },
            resolveDescriptorData(platformData)
        )
    }

    private fun TypeParameterDescriptor.toTypeParameter() =
        TypeParameter(
            DRI.from(this),
            fqNameSafe.asString(),
            PlatformDependent.from(platformData, getDocumentation()),
            upperBounds.map { it.toBound() },
            listOf(platformData),
            extra = additionalExtras()
        )

    private fun KotlinType.toBound(): Bound = when (constructor.declarationDescriptor) {
        is TypeParameterDescriptor -> OtherParameter(fqName.toString()).let {
            if (isMarkedNullable) Nullable(it) else it
        }
        else -> TypeConstructor(
            DRI.from(constructor.declarationDescriptor!!), // TODO: remove '!!'
            arguments.map { it.toProjection() }
        )
    }

    private fun TypeProjection.toProjection(): Projection =
        if (isStarProjection) Star else formPossiblyVariant()

    private fun TypeProjection.formPossiblyVariant(): Projection = type.fromPossiblyNullable().let {
        when (projectionKind) {
            org.jetbrains.kotlin.types.Variance.INVARIANT -> it
            org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> Variance(Variance.Kind.In, it)
            org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> Variance(Variance.Kind.Out, it)
        }
    }

    private fun KotlinType.fromPossiblyNullable(): Bound =
        toBound().let { if (isMarkedNullable) Nullable(it) else it }

    private fun DeclarationDescriptor.getDocumentation() = findKDoc().let {
        MarkdownParser(resolutionFacade, this).parseFromKDocTag(it)
    }

    fun ClassDescriptor.companion(dri: DRIWithPlatformInfo): Object? = companionObjectDescriptor?.let {
        objectDescriptor(it, dri)
    }

    fun MemberDescriptor.modifier() = when (modality) {
        Modality.FINAL -> KotlinModifier.Final
        Modality.SEALED -> KotlinModifier.Sealed
        Modality.OPEN -> KotlinModifier.Open
        Modality.ABSTRACT -> KotlinModifier.Abstract
        else -> KotlinModifier.Empty
    }

    private fun MemberDescriptor.createSources(): PlatformDependent<DocumentableSource> = if (isExpect()) {
        PlatformDependent(emptyMap(), DescriptorDocumentableSource(this))
    } else {
        PlatformDependent(mapOf(platformData to DescriptorDocumentableSource(this)))
    }

    inline fun <reified D : Documentable> FunctionDescriptor.additionalExtras(): PropertyContainer<D> = listOfNotNull(
        ExtraModifiers.DYNAMIC.takeIf { isDynamic() },
        ExtraModifiers.INFIX.takeIf { isInfix },
        ExtraModifiers.INLINE.takeIf { isInline },
        ExtraModifiers.SUSPEND.takeIf { isSuspend },
        ExtraModifiers.OPERATOR.takeIf { isOperator },
        ExtraModifiers.STATIC.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.TAILREC.takeIf { isTailrec },
        ExtraModifiers.EXTERNAL.takeIf { isExternal },
        ExtraModifiers.OVERRIDE.takeIf { DescriptorUtils.isOverride(this) }
    ).toContainer()

    inline fun <reified D : Documentable> ClassDescriptor.additionalExtras(): PropertyContainer<D> = listOfNotNull(
        ExtraModifiers.DYNAMIC.takeIf { isDynamic() },
        ExtraModifiers.INLINE.takeIf { isInline },
        ExtraModifiers.EXTERNAL.takeIf { isExternal },
        ExtraModifiers.INNER.takeIf { isInner },
        ExtraModifiers.DATA.takeIf { isData },
        ExtraModifiers.OVERRIDE.takeIf { getSuperInterfaces().isNotEmpty() || getSuperClassNotAny() != null }
    ).toContainer()

    fun ValueParameterDescriptor.additionalExtras(): PropertyContainer<Parameter> =
        listOfNotNull(
            ExtraModifiers.DYNAMIC.takeIf { isDynamic() },
            ExtraModifiers.NOINLINE.takeIf { isNoinline },
            ExtraModifiers.CROSSINLINE.takeIf { isCrossinline },
            ExtraModifiers.CONST.takeIf { isConst },
            ExtraModifiers.LATEINIT.takeIf { isLateInit },
            ExtraModifiers.VARARG.takeIf { isVararg }
        ).toContainer()

    fun TypeParameterDescriptor.additionalExtras(): PropertyContainer<TypeParameter> =
        listOfNotNull(
            ExtraModifiers.DYNAMIC.takeIf { isDynamic() },
            ExtraModifiers.REIFIED.takeIf { isReified }
        ).toContainer()

    fun PropertyDescriptor.additionalExtras(): PropertyContainer<Property> = listOfNotNull(
        ExtraModifiers.DYNAMIC.takeIf { isDynamic() },
        ExtraModifiers.CONST.takeIf { isConst },
        ExtraModifiers.LATEINIT.takeIf { isLateInit },
        ExtraModifiers.STATIC.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.EXTERNAL.takeIf { isExternal },
        ExtraModifiers.OVERRIDE.takeIf { DescriptorUtils.isOverride(this) }
    ).toContainer()

    inline fun <reified D : Documentable> List<ExtraModifiers>.toContainer(
        container: PropertyContainer<D> = PropertyContainer.empty()
    ): PropertyContainer<D> =
        container + AdditionalModifiers(this)

    data class ClassInfo(val supertypes: List<DRI>, val docs: PlatformDependent<DocumentationNode>)

    private fun Visibility.toDokkaVisibility(): org.jetbrains.dokka.model.Visibility = when (this) {
        Visibilities.PUBLIC -> KotlinVisibility.Public
        Visibilities.PROTECTED -> KotlinVisibility.Protected
        Visibilities.INTERNAL -> KotlinVisibility.Internal
        Visibilities.PRIVATE -> KotlinVisibility.Private
        else -> KotlinVisibility.Public
    }
}
