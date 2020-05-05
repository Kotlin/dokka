package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.parsers.MarkdownParser
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.codegen.isJvmStaticInObjectOrClassOrInterface
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object DefaultDescriptorToDocumentableTranslator : SourceToDocumentableTranslator {

    override fun invoke(platformData: PlatformData, context: DokkaContext): DModule {

        val (environment, facade) = context.platforms.getValue(platformData)
        val packageFragments = environment.getSourceFiles().asSequence()
            .map { it.packageFqName }
            .distinct()
            .mapNotNull { facade.resolveSession.getPackageFragment(it) }
            .toList()

        return DokkaDescriptorVisitor(platformData, context.platforms.getValue(platformData).facade, context.logger).run {
            packageFragments.mapNotNull { it.safeAs<PackageFragmentDescriptor>() }.map {
                visitPackageFragmentDescriptor(
                    it,
                    DRIWithPlatformInfo(DRI.topLevel, PlatformDependent.empty())
                )
            }
        }.let { DModule(platformData.name, it, PlatformDependent.empty(), listOf(platformData)) }
    }
}

data class DRIWithPlatformInfo(
    val dri: DRI,
    val actual: PlatformDependent<DocumentableSource>
)

fun DRI.withEmptyInfo() = DRIWithPlatformInfo(this, PlatformDependent.empty())

private class DokkaDescriptorVisitor(
    private val platformData: PlatformData,
    private val resolutionFacade: DokkaResolutionFacade,
    private val logger: DokkaLogger
) : DeclarationDescriptorVisitorEmptyBodies<Documentable, DRIWithPlatformInfo>() {
    override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor, parent: DRIWithPlatformInfo): Nothing {
        throw IllegalStateException("${javaClass.simpleName} should never enter ${descriptor.javaClass.simpleName}")
    }

    override fun visitPackageFragmentDescriptor(
        descriptor: PackageFragmentDescriptor,
        parent: DRIWithPlatformInfo
    ): DPackage {
        val name = descriptor.fqName.asString().takeUnless { it.isBlank() }
            ?: "[" + platformData.targets.joinToString(" ") + " root]"// TODO: error-prone, find a better way to do it
        val driWithPlatform = DRI(packageName = name).withEmptyInfo()
        val scope = descriptor.getMemberScope()

        return DPackage(
            dri = driWithPlatform.dri,
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            typealiases = scope.typealiases(driWithPlatform),
            documentation = descriptor.resolveDescriptorData(platformData),
            platformData = listOf(platformData)
        )
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DClasslike =
        when (descriptor.kind) {
            ClassKind.ENUM_CLASS -> enumDescriptor(descriptor, parent)
            ClassKind.OBJECT -> objectDescriptor(descriptor, parent)
            ClassKind.INTERFACE -> interfaceDescriptor(descriptor, parent)
            ClassKind.ANNOTATION_CLASS -> annotationDescriptor(descriptor, parent)
            else -> classDescriptor(descriptor, parent)
        }

    private fun interfaceDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DInterface {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val info = descriptor.resolveClassDescriptionData(if (!isExpect) platformData else null)


        return DInterface(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            sources = descriptor.createSources(),
            visibility = if (isExpect) PlatformDependent.expectFrom(descriptor.visibility.toDokkaVisibility())
            else PlatformDependent.from(platformData, descriptor.visibility.toDokkaVisibility()),
            supertypes = if (isExpect) PlatformDependent.expectFrom(info.supertypes)
            else PlatformDependent.from(platformData, info.supertypes),
            documentation = info.docs,
            generics = descriptor.typeConstructor.parameters.map { it.toTypeParameter() },
            companion = descriptor.companion(driWithPlatform),
            platformData = listOf(platformData),
            extra = PropertyContainer.withAll(descriptor.additionalExtras(), descriptor.getAnnotations())
        )
    }

    private fun objectDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DObject {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val info = descriptor.resolveClassDescriptionData(if (!isExpect) platformData else null)


        return DObject(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            sources = descriptor.createSources(),
            visibility = if (isExpect) PlatformDependent.expectFrom(descriptor.visibility.toDokkaVisibility())
            else PlatformDependent.from(platformData, descriptor.visibility.toDokkaVisibility()),
            supertypes = if (isExpect) PlatformDependent.expectFrom(info.supertypes)
            else PlatformDependent.from(platformData, info.supertypes),
            documentation = info.docs,
            platformData = listOf(platformData),
            extra = PropertyContainer.withAll(descriptor.additionalExtras(), descriptor.getAnnotations())
        )
    }

    private fun enumDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DEnum {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val info = descriptor.resolveClassDescriptionData(if (!isExpect) platformData else null)

        return DEnum(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            entries = scope.enumEntries(driWithPlatform),
            constructors = descriptor.constructors.map { visitConstructorDescriptor(it, driWithPlatform) },
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            sources = descriptor.createSources(),
            visibility = if (isExpect) PlatformDependent.expectFrom(descriptor.visibility.toDokkaVisibility())
            else PlatformDependent.from(platformData, descriptor.visibility.toDokkaVisibility()),
            supertypes = if (isExpect) PlatformDependent.expectFrom(info.supertypes)
            else PlatformDependent.from(platformData, info.supertypes),
            documentation = info.docs,
            companion = descriptor.companion(driWithPlatform),
            platformData = listOf(platformData),
            extra = PropertyContainer.withAll(descriptor.additionalExtras(), descriptor.getAnnotations())
        )
    }

    private fun enumEntryDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DEnumEntry {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect

        return DEnumEntry(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            documentation = descriptor.resolveDescriptorData(if (!isExpect) platformData else null),
            classlikes = scope.classlikes(driWithPlatform),
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            platformData = listOf(platformData),
            extra = PropertyContainer.withAll(descriptor.additionalExtras(), descriptor.getAnnotations())
        )
    }

    fun annotationDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DAnnotation {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope

        return DAnnotation(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            documentation = descriptor.resolveDescriptorData(platformData),
            classlikes = scope.classlikes(driWithPlatform),
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            platformData = listOf(platformData),
            extra = PropertyContainer.withAll(descriptor.additionalExtras(), descriptor.getAnnotations()),
            companion = descriptor.companionObjectDescriptor?.let { objectDescriptor(it, driWithPlatform) },
            visibility = PlatformDependent(mapOf(platformData to descriptor.visibility.toDokkaVisibility())),
            constructors = descriptor.constructors.map { visitConstructorDescriptor(it, driWithPlatform) },
            sources = descriptor.createSources()
        )
    }

    private fun classDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DClass {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val info = descriptor.resolveClassDescriptionData(if (!isExpect) platformData else null)
        val actual = descriptor.createSources()

        return DClass(
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
            visibility = if (isExpect) PlatformDependent.expectFrom(descriptor.visibility.toDokkaVisibility())
            else PlatformDependent.from(platformData, descriptor.visibility.toDokkaVisibility()),
            supertypes = if (isExpect) PlatformDependent.expectFrom(info.supertypes)
            else PlatformDependent.from(platformData, info.supertypes),
            generics = descriptor.typeConstructor.parameters.map { it.toTypeParameter() },
            documentation = info.docs,
            modifier = if (isExpect) PlatformDependent.expectFrom(descriptor.modifier())
            else PlatformDependent.from(platformData, descriptor.modifier()),
            companion = descriptor.companion(driWithPlatform),
            platformData = listOf(platformData),
            extra = PropertyContainer.withAll(descriptor.additionalExtras(), descriptor.getAnnotations())
        )
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, parent: DRIWithPlatformInfo): DProperty {
        val dri = parent.dri.copy(callable = Callable.from(descriptor))
        val isExpect = descriptor.isExpect

        val actual = descriptor.createSources()
        return DProperty(
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
            visibility = if (isExpect) PlatformDependent.expectFrom(descriptor.visibility.toDokkaVisibility())
            else PlatformDependent.from(platformData, descriptor.visibility.toDokkaVisibility()),
            documentation = descriptor.resolveDescriptorData(if (!isExpect) platformData else null),
            modifier = if (isExpect) PlatformDependent.expectFrom(descriptor.modifier())
            else PlatformDependent.from(platformData, descriptor.modifier()),
            type = descriptor.returnType!!.toBound(),
            platformData = listOf(platformData),
            generics = descriptor.typeParameters.map { it.toTypeParameter() },
            extra = PropertyContainer.withAll(descriptor.additionalExtras(), descriptor.getAnnotations())
        )
    }

    fun CallableMemberDescriptor.createDRI(wasOverriden: Boolean = false): Pair<DRI, Boolean> =
        if (kind == CallableMemberDescriptor.Kind.DECLARATION || overriddenDescriptors.isEmpty())
            Pair(DRI.from(this), wasOverriden)
        else
            overriddenDescriptors.first().createDRI(true)

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, parent: DRIWithPlatformInfo): DFunction {
        val (dri, isInherited) = descriptor.createDRI()
        val isExpect = descriptor.isExpect

        val actual = descriptor.createSources()
        return DFunction(
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
            visibility = if (isExpect) PlatformDependent.expectFrom(descriptor.visibility.toDokkaVisibility())
            else PlatformDependent.from(platformData, descriptor.visibility.toDokkaVisibility()),
            generics = descriptor.typeParameters.map { it.toTypeParameter() },
            documentation = descriptor.resolveDescriptorData(if (!isExpect) platformData else null),
            modifier = if (isExpect) PlatformDependent.expectFrom(descriptor.modifier())
            else PlatformDependent.from(platformData, descriptor.modifier()),
            type = descriptor.returnType!!.toBound(),
            platformData = listOf(platformData),
            extra = PropertyContainer.withAll(
                InheritedFunction(isInherited),
                descriptor.additionalExtras(), descriptor.getAnnotations()
            )
        )
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, parent: DRIWithPlatformInfo): DFunction {
        val dri = parent.dri.copy(callable = Callable.from(descriptor))
        val actual = descriptor.createSources()
        val isExpect = descriptor.isExpect

        return DFunction(
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
            visibility = if (isExpect) PlatformDependent.expectFrom(descriptor.visibility.toDokkaVisibility())
            else PlatformDependent.from(platformData, descriptor.visibility.toDokkaVisibility()),
            documentation = descriptor.resolveDescriptorData(if (!isExpect) platformData else null).let {
                if (descriptor.isPrimary) {
                    it.copy(
                        map = PlatformDependent.from(it.map.map {
                            Pair(
                                it.key,
                                it.value.copy(children = (it.value.children.find { it is Constructor }?.root?.let { constructor ->
                                    listOf(
                                        Description(constructor)
                                    )
                                } ?: emptyList<TagWrapper>()) + it.value.children.filterIsInstance<Param>())
                            )
                        }),
                        expect = it.expect?.copy(children = (it.expect?.children?.find { it is Constructor }?.root?.let { constructor ->
                            listOf(
                                Description(constructor)
                            )
                        } ?: emptyList<TagWrapper>()) + it.expect!!.children.filterIsInstance<Param>())
                    )
                } else {
                    it
                }
            },
            type = descriptor.returnType.toBound(),
            modifier = if (isExpect) PlatformDependent.expectFrom(descriptor.modifier())
            else PlatformDependent.from(platformData, descriptor.modifier()),
            generics = descriptor.typeParameters.map { it.toTypeParameter() },
            platformData = listOf(platformData),
            extra = PropertyContainer.withAll<DFunction>(descriptor.additionalExtras(), descriptor.getAnnotations())
                .let {
                    if (descriptor.isPrimary) {
                        it + PrimaryConstructorExtra
                    } else it
                }
        )
    }

    override fun visitReceiverParameterDescriptor(
        descriptor: ReceiverParameterDescriptor,
        parent: DRIWithPlatformInfo
    ) = DParameter(
        dri = parent.dri.copy(target = 0),
        name = null,
        type = descriptor.type.toBound(),
        documentation = descriptor.resolveDescriptorData(platformData),
        platformData = listOf(platformData)
    )

    private fun visitPropertyAccessorDescriptor(
        descriptor: PropertyAccessorDescriptor,
        propertyDescriptor: PropertyDescriptor,
        parent: DRI
    ): DFunction {
        val dri = parent.copy(callable = Callable.from(descriptor))
        val isGetter = descriptor is PropertyGetterDescriptor
        val isExpect = descriptor.isExpect

        fun PropertyDescriptor.asParameter(parent: DRI) =
            DParameter(
                parent.copy(target = 1),
                this.name.asString(),
                type = this.type.toBound(),
                documentation = descriptor.resolveDescriptorData(if (!isExpect) platformData else null),
                platformData = listOf(platformData),
                extra = PropertyContainer.withAll(descriptor.additionalExtras(), descriptor.getAnnotations())
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

        return DFunction(
            dri,
            name,
            isConstructor = false,
            parameters = parameters,
            visibility = if (isExpect) PlatformDependent.expectFrom(descriptor.visibility.toDokkaVisibility())
            else PlatformDependent.from(platformData, descriptor.visibility.toDokkaVisibility()),
            documentation = descriptor.resolveDescriptorData(if (!isExpect) platformData else null),
            type = descriptor.returnType!!.toBound(),
            generics = descriptor.typeParameters.map { it.toTypeParameter() },
            modifier = if (isExpect) PlatformDependent.expectFrom(descriptor.modifier())
            else PlatformDependent.from(platformData, descriptor.modifier()),
            receiver = descriptor.extensionReceiverParameter?.let {
                visitReceiverParameterDescriptor(
                    it,
                    DRIWithPlatformInfo(dri, descriptor.createSources())
                )
            },
            sources = descriptor.createSources(),
            platformData = listOf(platformData),
            extra = PropertyContainer.withAll(descriptor.additionalExtras(), descriptor.getAnnotations())
        )
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, parent: DRIWithPlatformInfo?) =
        with(descriptor) {
            DTypeAlias(
                dri = DRI.from(this),
                name = name.asString(),
                type = defaultType.toBound(),
                underlyingType = PlatformDependent.from(platformData, underlyingType.toBound()),
                visibility = if (isExpect) PlatformDependent.expectFrom(visibility.toDokkaVisibility())
                else PlatformDependent.from(platformData, visibility.toDokkaVisibility()),
                documentation = resolveDescriptorData(platformData),
                platformData = listOf(platformData)
            )
        }

    private fun parameter(index: Int, descriptor: ValueParameterDescriptor, parent: DRIWithPlatformInfo) =
        DParameter(
            dri = parent.dri.copy(target = index + 1),
            name = descriptor.name.asString(),
            type = descriptor.type.toBound(),
            documentation = descriptor.resolveDescriptorData(platformData),
            platformData = listOf(platformData),
            extra = PropertyContainer.withAll(
                listOfNotNull(
                    descriptor.additionalExtras(),
                    descriptor.getAnnotations(),
                    descriptor.getDefaultValue()?.let { DefaultValue(it) })
            )
        )

    private fun MemberScope.functions(parent: DRIWithPlatformInfo): List<DFunction> =
        getContributedDescriptors(DescriptorKindFilter.FUNCTIONS) { true }
            .filterIsInstance<FunctionDescriptor>()
            .map { visitFunctionDescriptor(it, parent) }

    private fun MemberScope.properties(parent: DRIWithPlatformInfo): List<DProperty> =
        getContributedDescriptors(DescriptorKindFilter.VALUES) { true }
            .filterIsInstance<PropertyDescriptor>()
            .map { visitPropertyDescriptor(it, parent) }

    private fun MemberScope.classlikes(parent: DRIWithPlatformInfo): List<DClasslike> =
        getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) { true }
            .filter { it is ClassDescriptor && it.kind != ClassKind.ENUM_ENTRY }
            .map { visitClassDescriptor(it as ClassDescriptor, parent) }
            .mapNotNull { it as? DClasslike }

    private fun MemberScope.packages(parent: DRIWithPlatformInfo): List<DPackage> =
        getContributedDescriptors(DescriptorKindFilter.PACKAGES) { true }
            .filterIsInstance<PackageFragmentDescriptor>()
            .map { visitPackageFragmentDescriptor(it, parent) }

    private fun MemberScope.typealiases(parent: DRIWithPlatformInfo): List<DTypeAlias> =
        getContributedDescriptors(DescriptorKindFilter.TYPE_ALIASES) { true }
            .filterIsInstance<TypeAliasDescriptor>()
            .map { visitTypeAliasDescriptor(it, parent) }

    private fun MemberScope.enumEntries(parent: DRIWithPlatformInfo): List<DEnumEntry> =
        this.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) { true }
            .filterIsInstance<ClassDescriptor>()
            .filter { it.kind == ClassKind.ENUM_ENTRY }
            .map { enumEntryDescriptor(it, parent) }


    private fun DeclarationDescriptor.resolveDescriptorData(platformData: PlatformData?): PlatformDependent<DocumentationNode> =
        if (platformData != null) PlatformDependent.from(
            platformData,
            getDocumentation()
        ) else PlatformDependent.expectFrom(getDocumentation())

    private fun ClassDescriptor.resolveClassDescriptionData(platformData: PlatformData?): ClassInfo {
        return ClassInfo(
            (getSuperInterfaces() + getAllSuperclassesWithoutAny()).map { DRI.from(it) },
            resolveDescriptorData(platformData)
        )
    }

    private fun TypeParameterDescriptor.toTypeParameter() =
        DTypeParameter(
            DRI.from(this),
            name.identifier,
            PlatformDependent.from(platformData, getDocumentation()),
            upperBounds.map { it.toBound() },
            listOf(platformData),
            extra = PropertyContainer.withAll(additionalExtras())
        )

    private fun KotlinType.toBound(): Bound = when (val ctor = constructor.declarationDescriptor) {
        is TypeParameterDescriptor -> OtherParameter(ctor.name.asString()).let {
            if (isMarkedNullable) Nullable(it) else it
        }
        else -> TypeConstructor(
            DRI.from(constructor.declarationDescriptor!!), // TODO: remove '!!'
            arguments.map { it.toProjection() },
            if (isExtensionFunctionType) FunctionModifiers.EXTENSION
            else if (isFunctionType) FunctionModifiers.FUNCTION
            else FunctionModifiers.NONE
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
        MarkdownParser(resolutionFacade, this, logger).parseFromKDocTag(it)
    }

    private fun ClassDescriptor.companion(dri: DRIWithPlatformInfo): DObject? = companionObjectDescriptor?.let {
        objectDescriptor(it, dri)
    }

    private fun MemberDescriptor.modifier() = when (modality) {
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

    private fun FunctionDescriptor.additionalExtras() = listOfNotNull(
        ExtraModifiers.DYNAMIC.takeIf { isDynamic() },
        ExtraModifiers.INFIX.takeIf { isInfix },
        ExtraModifiers.INLINE.takeIf { isInline },
        ExtraModifiers.SUSPEND.takeIf { isSuspend },
        ExtraModifiers.OPERATOR.takeIf { isOperator },
        ExtraModifiers.STATIC.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.TAILREC.takeIf { isTailrec },
        ExtraModifiers.EXTERNAL.takeIf { isExternal },
        ExtraModifiers.OVERRIDE.takeIf { DescriptorUtils.isOverride(this) }
    ).toProperty()

    private fun ClassDescriptor.additionalExtras() = listOfNotNull(
        ExtraModifiers.DYNAMIC.takeIf { isDynamic() },
        ExtraModifiers.INLINE.takeIf { isInline },
        ExtraModifiers.EXTERNAL.takeIf { isExternal },
        ExtraModifiers.INNER.takeIf { isInner },
        ExtraModifiers.DATA.takeIf { isData },
        ExtraModifiers.OVERRIDE.takeIf { getSuperInterfaces().isNotEmpty() || getSuperClassNotAny() != null }
    ).toProperty()

    private fun ValueParameterDescriptor.additionalExtras() =
        listOfNotNull(
            ExtraModifiers.DYNAMIC.takeIf { isDynamic() },
            ExtraModifiers.NOINLINE.takeIf { isNoinline },
            ExtraModifiers.CROSSINLINE.takeIf { isCrossinline },
            ExtraModifiers.CONST.takeIf { isConst },
            ExtraModifiers.LATEINIT.takeIf { isLateInit },
            ExtraModifiers.VARARG.takeIf { isVararg }
        ).toProperty()

    private fun TypeParameterDescriptor.additionalExtras() =
        listOfNotNull(
            ExtraModifiers.DYNAMIC.takeIf { isDynamic() },
            ExtraModifiers.REIFIED.takeIf { isReified }
        ).toProperty()

    private fun PropertyDescriptor.additionalExtras() = listOfNotNull(
        ExtraModifiers.DYNAMIC.takeIf { isDynamic() },
        ExtraModifiers.CONST.takeIf { isConst },
        ExtraModifiers.LATEINIT.takeIf { isLateInit },
        ExtraModifiers.STATIC.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.EXTERNAL.takeIf { isExternal },
        ExtraModifiers.OVERRIDE.takeIf { DescriptorUtils.isOverride(this) }
    ).toProperty()

    private fun List<ExtraModifiers>.toProperty() =
        AdditionalModifiers(this.toSet())

    private fun DeclarationDescriptor.getAnnotations() = annotations.map { annotation ->
        Annotations.Annotation(
            annotation.let { it.annotationClass as DeclarationDescriptor }.let { DRI.from(it) },
            annotation.allValueArguments.map { (k, v) -> k.asString() to v.value.toString() }.toMap()
        )
    }.let(::Annotations)

    private fun ValueParameterDescriptor.getDefaultValue(): String? =
        (source as? KotlinSourceElement)?.psi?.children?.find { it is KtExpression }?.text

    private data class ClassInfo(val supertypes: List<DRI>, val docs: PlatformDependent<DocumentationNode>)

    private fun Visibility.toDokkaVisibility(): org.jetbrains.dokka.model.Visibility = when (this) {
        Visibilities.PUBLIC -> KotlinVisibility.Public
        Visibilities.PROTECTED -> KotlinVisibility.Protected
        Visibilities.INTERNAL -> KotlinVisibility.Internal
        Visibilities.PRIVATE -> KotlinVisibility.Private
        else -> KotlinVisibility.Public
    }
}
