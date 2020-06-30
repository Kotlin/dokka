package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.*
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.base.parsers.MarkdownParser
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.codegen.isJvmStaticInObjectOrClassOrInterface
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentsInParentheses
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.KClassValue.Value.LocalClass
import org.jetbrains.kotlin.resolve.constants.KClassValue.Value.NormalClass
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.DynamicType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.nio.file.Paths
import org.jetbrains.kotlin.resolve.constants.AnnotationValue as ConstantsAnnotationValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue as ConstantsArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue as ConstantsEnumValue
import org.jetbrains.kotlin.resolve.constants.KClassValue as ConstantsKtClassValue

class DefaultDescriptorToDocumentableTranslator(
    private val kotlinAnalysis: KotlinAnalysis
) : SourceToDocumentableTranslator {

    override fun invoke(sourceSet: DokkaSourceSet, context: DokkaContext): DModule {
        val (environment, facade) = kotlinAnalysis[sourceSet]
        val packageFragments = environment.getSourceFiles().asSequence()
            .map { it.packageFqName }
            .distinct()
            .mapNotNull { facade.resolveSession.getPackageFragment(it) }
            .toList()

        return DokkaDescriptorVisitor(sourceSet, kotlinAnalysis[sourceSet].facade, context.logger).run {
            packageFragments.mapNotNull { it.safeAs<PackageFragmentDescriptor>() }.map {
                visitPackageFragmentDescriptor(
                    it,
                    DRIWithPlatformInfo(DRI.topLevel, emptyMap())
                )
            }
        }.let { DModule(sourceSet.moduleDisplayName, it, emptyMap(), null, setOf(sourceSet)) }
    }
}

data class DRIWithPlatformInfo(
    val dri: DRI,
    val actual: SourceSetDependent<DocumentableSource>
)

fun DRI.withEmptyInfo() = DRIWithPlatformInfo(this, emptyMap())

private class DokkaDescriptorVisitor(
    private val sourceSet: DokkaSourceSet,
    private val resolutionFacade: DokkaResolutionFacade,
    private val logger: DokkaLogger
) : DeclarationDescriptorVisitorEmptyBodies<Documentable, DRIWithPlatformInfo>() {
    override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor, parent: DRIWithPlatformInfo): Nothing {
        throw IllegalStateException("${javaClass.simpleName} should never enter ${descriptor.javaClass.simpleName}")
    }

    private fun Collection<DeclarationDescriptor>.filterDescriptorsInSourceSet() = filter {
        it.toSourceElement.containingFile.toString().let { path ->
            path.isNotBlank() && sourceSet.sourceRoots.any { root ->
                Paths.get(path).startsWith(Paths.get(root.path))
            }
        }
    }

    private fun <T> T.toSourceSetDependent() = mapOf(sourceSet to this)

    override fun visitPackageFragmentDescriptor(
        descriptor: PackageFragmentDescriptor,
        parent: DRIWithPlatformInfo
    ): DPackage {
        val name = descriptor.fqName.asString().takeUnless { it.isBlank() } ?: fallbackPackageName()
        val driWithPlatform = DRI(packageName = name).withEmptyInfo()
        val scope = descriptor.getMemberScope()

        return DPackage(
            dri = driWithPlatform.dri,
            functions = scope.functions(driWithPlatform, true),
            properties = scope.properties(driWithPlatform, true),
            classlikes = scope.classlikes(driWithPlatform, true),
            typealiases = scope.typealiases(driWithPlatform, true),
            documentation = descriptor.resolveDescriptorData(),
            sourceSets = setOf(sourceSet)
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
        val info = descriptor.resolveClassDescriptionData()

        return DInterface(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            sources = descriptor.createSources(),
            expectPresentInSet = sourceSet.takeIf { isExpect },
            visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
            supertypes = info.supertypes.toSourceSetDependent(),
            documentation = info.docs,
            generics = descriptor.declaredTypeParameters.map { it.toTypeParameter() },
            companion = descriptor.companion(driWithPlatform),
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
                descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                ImplementedInterfaces(info.interfaces.toSourceSetDependent())
            )
        )
    }

    private fun objectDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DObject {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val info = descriptor.resolveClassDescriptionData()


        return DObject(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            sources = descriptor.createSources(),
            expectPresentInSet = sourceSet.takeIf { isExpect },
            visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
            supertypes = info.supertypes.toSourceSetDependent(),
            documentation = info.docs,
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
                descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                ImplementedInterfaces(info.interfaces.toSourceSetDependent())
            )
        )
    }

    private fun enumDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DEnum {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val info = descriptor.resolveClassDescriptionData()

        return DEnum(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            entries = scope.enumEntries(driWithPlatform),
            constructors = descriptor.constructors.map { visitConstructorDescriptor(it, driWithPlatform) },
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            sources = descriptor.createSources(),
            expectPresentInSet = sourceSet.takeIf { isExpect },
            visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
            supertypes = info.supertypes.toSourceSetDependent(),
            documentation = info.docs,
            companion = descriptor.companion(driWithPlatform),
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
                descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                ImplementedInterfaces(info.interfaces.toSourceSetDependent())
            )
        )
    }

    private fun enumEntryDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DEnumEntry {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect

        return DEnumEntry(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            documentation = descriptor.resolveDescriptorData(),
            classlikes = scope.classlikes(driWithPlatform),
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            sourceSets = setOf(sourceSet),
            expectPresentInSet = sourceSet.takeIf { isExpect },
            extra = PropertyContainer.withAll(
                descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                ConstructorValues(descriptor.getAppliedConstructorParameters().toSourceSetDependent())
            )
        )
    }

    fun annotationDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DAnnotation {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope

        return DAnnotation(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            documentation = descriptor.resolveDescriptorData(),
            classlikes = scope.classlikes(driWithPlatform),
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            expectPresentInSet = null,
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
                descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotations().toSourceSetDependent().toAnnotations()
            ),
            companion = descriptor.companionObjectDescriptor?.let { objectDescriptor(it, driWithPlatform) },
            visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
            generics = descriptor.declaredTypeParameters.map { it.toTypeParameter() },
            constructors = descriptor.constructors.map { visitConstructorDescriptor(it, driWithPlatform) },
            sources = descriptor.createSources()
        )
    }

    private fun classDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DClass {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val info = descriptor.resolveClassDescriptionData()
        val actual = descriptor.createSources()

        return DClass(
            dri = driWithPlatform.dri,
            name = descriptor.name.asString(),
            constructors = descriptor.constructors.map {
                visitConstructorDescriptor(
                    it,
                    if (it.isPrimary) DRIWithPlatformInfo(driWithPlatform.dri, actual)
                    else DRIWithPlatformInfo(driWithPlatform.dri, emptyMap())
                )
            },
            functions = scope.functions(driWithPlatform),
            properties = scope.properties(driWithPlatform),
            classlikes = scope.classlikes(driWithPlatform),
            sources = actual,
            expectPresentInSet = sourceSet.takeIf { isExpect },
            visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
            supertypes = info.supertypes.toSourceSetDependent(),
            generics = descriptor.declaredTypeParameters.map { it.toTypeParameter() },
            documentation = info.docs,
            modifier = descriptor.modifier().toSourceSetDependent(),
            companion = descriptor.companion(driWithPlatform),
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
                descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                ImplementedInterfaces(info.interfaces.toSourceSetDependent())
            )
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
            visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
            documentation = descriptor.resolveDescriptorData(),
            modifier = descriptor.modifier().toSourceSetDependent(),
            type = descriptor.returnType!!.toBound(),
            expectPresentInSet = sourceSet.takeIf { isExpect },
            sourceSets = setOf(sourceSet),
            generics = descriptor.typeParameters.map { it.toTypeParameter() },
            extra = PropertyContainer.withAll(
                (descriptor.additionalExtras() + descriptor.getAnnotationsWithBackingField()
                    .toAdditionalExtras()).toSet().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotationsWithBackingField().toSourceSetDependent().toAnnotations()
            )
        )
    }

    fun CallableMemberDescriptor.createDRI(wasOverridenBy: DRI? = null): Pair<DRI, DRI?> =
        if (kind == CallableMemberDescriptor.Kind.DECLARATION || overriddenDescriptors.isEmpty())
            Pair(DRI.from(this), wasOverridenBy)
        else
            overriddenDescriptors.first().createDRI(DRI.from(this))

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, parent: DRIWithPlatformInfo): DFunction {
        val (dri, inheritedFrom) = descriptor.createDRI()
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
            expectPresentInSet = sourceSet.takeIf { isExpect },
            sources = actual,
            visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
            generics = descriptor.typeParameters.map { it.toTypeParameter() },
            documentation = descriptor.resolveDescriptorData(),
            modifier = descriptor.modifier().toSourceSetDependent(),
            type = descriptor.returnType!!.toBound(),
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
                InheritedFunction(inheritedFrom.toSourceSetDependent()),
                descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotations().toSourceSetDependent().toAnnotations()
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
            expectPresentInSet = sourceSet.takeIf { isExpect },
            visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
            documentation = descriptor.resolveDescriptorData().let { sourceSetDependent ->
                if (descriptor.isPrimary) {
                    sourceSetDependent.map { entry ->
                        Pair(
                            entry.key,
                            entry.value.copy(children = (entry.value.children.find { it is Constructor }?.root?.let { constructor ->
                                listOf(Description(constructor))
                            } ?: emptyList<TagWrapper>()) + entry.value.children.filterIsInstance<Param>()))
                    }.toMap()
                } else {
                    sourceSetDependent
                }
            },
            type = descriptor.returnType.toBound(),
            modifier = descriptor.modifier().toSourceSetDependent(),
            generics = descriptor.typeParameters.map { it.toTypeParameter() },
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll<DFunction>(
                descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotations().toSourceSetDependent().toAnnotations()
            ).let {
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
        dri = parent.dri.copy(target = PointingToDeclaration),
        name = null,
        type = descriptor.type.toBound(),
        expectPresentInSet = null,
        documentation = descriptor.resolveDescriptorData(),
        sourceSets = setOf(sourceSet),
        extra = PropertyContainer.withAll(descriptor.getAnnotations().toSourceSetDependent().toAnnotations())
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
                parent.copy(target = PointingToCallableParameters(parameterIndex = 1)),
                this.name.asString(),
                type = this.type.toBound(),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                documentation = descriptor.resolveDescriptorData(),
                sourceSets = setOf(sourceSet),
                extra = PropertyContainer.withAll(
                    descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                    getAnnotationsWithBackingField().toSourceSetDependent().toAnnotations()
                )
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
            visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
            documentation = descriptor.resolveDescriptorData(),
            type = descriptor.returnType!!.toBound(),
            generics = descriptor.typeParameters.map { it.toTypeParameter() },
            modifier = descriptor.modifier().toSourceSetDependent(),
            expectPresentInSet = sourceSet.takeIf { isExpect },
            receiver = descriptor.extensionReceiverParameter?.let {
                visitReceiverParameterDescriptor(
                    it,
                    DRIWithPlatformInfo(dri, descriptor.createSources())
                )
            },
            sources = descriptor.createSources(),
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(
                descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotations().toSourceSetDependent().toAnnotations()
            )
        )
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, parent: DRIWithPlatformInfo?) =
        with(descriptor) {
            DTypeAlias(
                dri = DRI.from(this),
                name = name.asString(),
                type = defaultType.toBound(),
                expectPresentInSet = null,
                underlyingType = underlyingType.toBound().toSourceSetDependent(),
                visibility = visibility.toDokkaVisibility().toSourceSetDependent(),
                documentation = resolveDescriptorData(),
                sourceSets = setOf(sourceSet)
            )
        }

    private fun parameter(index: Int, descriptor: ValueParameterDescriptor, parent: DRIWithPlatformInfo) =
        DParameter(
            dri = parent.dri.copy(target = PointingToCallableParameters(index)),
            name = descriptor.name.asString(),
            type = descriptor.type.toBound(),
            expectPresentInSet = null,
            documentation = descriptor.resolveDescriptorData(),
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(listOfNotNull(
                descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                descriptor.getDefaultValue()?.let { DefaultValue(it) }
            ))
        )

    private fun MemberScope.getContributedDescriptors(kindFilter: DescriptorKindFilter, shouldFilter: Boolean) =
        getContributedDescriptors(kindFilter) { true }.let {
            if (shouldFilter) it.filterDescriptorsInSourceSet() else it
        }

    private fun MemberScope.functions(parent: DRIWithPlatformInfo, packageLevel: Boolean = false): List<DFunction> =
        getContributedDescriptors(DescriptorKindFilter.FUNCTIONS, packageLevel)
            .filterIsInstance<FunctionDescriptor>()
            .map { visitFunctionDescriptor(it, parent) }

    private fun MemberScope.properties(parent: DRIWithPlatformInfo, packageLevel: Boolean = false): List<DProperty> =
        getContributedDescriptors(DescriptorKindFilter.VALUES, packageLevel)
            .filterIsInstance<PropertyDescriptor>()
            .map { visitPropertyDescriptor(it, parent) }

    private fun MemberScope.classlikes(parent: DRIWithPlatformInfo, packageLevel: Boolean = false): List<DClasslike> =
        getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, packageLevel)
            .filter { it is ClassDescriptor && it.kind != ClassKind.ENUM_ENTRY }
            .map { visitClassDescriptor(it as ClassDescriptor, parent) }

    private fun MemberScope.packages(parent: DRIWithPlatformInfo): List<DPackage> =
        getContributedDescriptors(DescriptorKindFilter.PACKAGES) { true }
            .filterIsInstance<PackageFragmentDescriptor>()
            .map { visitPackageFragmentDescriptor(it, parent) }

    private fun MemberScope.typealiases(parent: DRIWithPlatformInfo, packageLevel: Boolean = false): List<DTypeAlias> =
        getContributedDescriptors(DescriptorKindFilter.TYPE_ALIASES, packageLevel)
            .filterIsInstance<TypeAliasDescriptor>()
            .map { visitTypeAliasDescriptor(it, parent) }

    private fun MemberScope.enumEntries(parent: DRIWithPlatformInfo): List<DEnumEntry> =
        this.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) { true }
            .filterIsInstance<ClassDescriptor>()
            .filter { it.kind == ClassKind.ENUM_ENTRY }
            .map { enumEntryDescriptor(it, parent) }


    private fun DeclarationDescriptor.resolveDescriptorData(): SourceSetDependent<DocumentationNode> =
        getDocumentation()?.toSourceSetDependent() ?: emptyMap()

    private fun ClassDescriptor.resolveClassDescriptionData(): ClassInfo {
        val interfaces = hashSetOf<DRI>()
        fun processSuperClasses(supers: List<ClassDescriptor>) {
            supers.forEach {
                if(it.kind == ClassKind.INTERFACE) interfaces.add(DRI.from(it))
                processSuperClasses(it.getSuperInterfaces() + it.getAllSuperclassesWithoutAny())
            }
        }
        processSuperClasses(getSuperInterfaces() + getAllSuperclassesWithoutAny())
        return ClassInfo(getAllSuperclassesWithoutAny().map { DRI.from(it) }, interfaces.toList(), resolveDescriptorData())
    }

    private fun TypeParameterDescriptor.toTypeParameter() =
        DTypeParameter(
            DRI.from(this),
            name.identifier,
            resolveDescriptorData(),
            null,
            upperBounds.map { it.toBound() },
            setOf(sourceSet),
            extra = PropertyContainer.withAll(additionalExtras().toSourceSetDependent().toAdditionalModifiers())
        )

    private fun KotlinType.toBound(): Bound = when (this) {
        is DynamicType -> Dynamic
        else -> when (val ctor = constructor.declarationDescriptor) {
            is TypeParameterDescriptor -> OtherParameter(
                declarationDRI = DRI.from(ctor.containingDeclaration).withPackageFallbackTo(fallbackPackageName()),
                name = ctor.name.asString()
            )
            else -> TypeConstructor(
                DRI.from(constructor.declarationDescriptor!!), // TODO: remove '!!'
                arguments.map { it.toProjection() },
                if (isExtensionFunctionType) FunctionModifiers.EXTENSION
                else if (isFunctionType) FunctionModifiers.FUNCTION
                else FunctionModifiers.NONE
            )
        }.let {
            if (isMarkedNullable) Nullable(it) else it
        }
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
    }.takeIf { it.children.isNotEmpty() }

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

    private fun MemberDescriptor.createSources(): SourceSetDependent<DocumentableSource> =
        DescriptorDocumentableSource(this).toSourceSetDependent()

    private fun FunctionDescriptor.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Infix.takeIf { isInfix },
        ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { isInline },
        ExtraModifiers.KotlinOnlyModifiers.Suspend.takeIf { isSuspend },
        ExtraModifiers.KotlinOnlyModifiers.Operator.takeIf { isOperator },
        ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.KotlinOnlyModifiers.TailRec.takeIf { isTailrec },
        ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
        ExtraModifiers.KotlinOnlyModifiers.Override.takeIf { DescriptorUtils.isOverride(this) }
    ).toSet()

    private fun ClassDescriptor.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Inline.takeIf { isInline },
        ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
        ExtraModifiers.KotlinOnlyModifiers.Inner.takeIf { isInner },
        ExtraModifiers.KotlinOnlyModifiers.Data.takeIf { isData },
        ExtraModifiers.KotlinOnlyModifiers.Fun.takeIf { isFun }
    ).toSet()

    private fun ValueParameterDescriptor.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.NoInline.takeIf { isNoinline },
        ExtraModifiers.KotlinOnlyModifiers.CrossInline.takeIf { isCrossinline },
        ExtraModifiers.KotlinOnlyModifiers.Const.takeIf { isConst },
        ExtraModifiers.KotlinOnlyModifiers.LateInit.takeIf { isLateInit },
        ExtraModifiers.KotlinOnlyModifiers.VarArg.takeIf { isVararg }
    ).toSet()

    private fun TypeParameterDescriptor.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Reified.takeIf { isReified }
    ).toSet()

    private fun PropertyDescriptor.additionalExtras() = listOfNotNull(
        ExtraModifiers.KotlinOnlyModifiers.Const.takeIf { isConst },
        ExtraModifiers.KotlinOnlyModifiers.LateInit.takeIf { isLateInit },
        ExtraModifiers.JavaOnlyModifiers.Static.takeIf { isJvmStaticInObjectOrClassOrInterface() },
        ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
        ExtraModifiers.KotlinOnlyModifiers.Override.takeIf { DescriptorUtils.isOverride(this) }
    )

    private fun Annotated.getAnnotations() = annotations.mapNotNull { it.toAnnotation() }

    private fun ConstantValue<*>.toValue(): AnnotationParameterValue? = when (this) {
        is ConstantsAnnotationValue -> value.toAnnotation()?.let { AnnotationValue(it) }
        is ConstantsArrayValue -> ArrayValue(value.mapNotNull { it.toValue() })
        is ConstantsEnumValue -> EnumValue(
            fullEnumEntryName(),
            DRI(enumClassId.packageFqName.asString(), fullEnumEntryName())
        )
        is ConstantsKtClassValue -> when (value) {
            is NormalClass -> (value as NormalClass).value.classId.let {
                ClassValue(
                    it.relativeClassName.asString(),
                    DRI(it.packageFqName.asString(), it.relativeClassName.asString())
                )
            }
            is LocalClass -> (value as LocalClass).type.let {
                ClassValue(
                    it.toString(),
                    DRI.from(it.constructor.declarationDescriptor as DeclarationDescriptor)
                )
            }
        }
        else -> StringValue(toString())
    }

    private fun AnnotationDescriptor.toAnnotation(): Annotations.Annotation {
        return Annotations.Annotation(
            DRI.from(annotationClass as DeclarationDescriptor),
            allValueArguments.map { it.key.asString() to it.value.toValue() }.filter {
                it.second != null
            }.toMap() as Map<String, AnnotationParameterValue>,
            annotationClass!!.annotations.hasAnnotation(FqName("kotlin.annotation.MustBeDocumented"))
        )
    }

    private fun PropertyDescriptor.getAnnotationsWithBackingField(): List<Annotations.Annotation> =
        getAnnotations() + (backingField?.getAnnotations() ?: emptyList())

    private fun List<Annotations.Annotation>.toAdditionalExtras() = mapNotNull {
        try {
            ExtraModifiers.valueOf(it.dri.classNames?.toLowerCase() ?: "")
        } catch (e: IllegalArgumentException) {
            null
        }
    }


    private fun ValueParameterDescriptor.getDefaultValue(): String? =
        (source as? KotlinSourceElement)?.psi?.children?.find { it is KtExpression }?.text

    private fun ClassDescriptor.getAppliedConstructorParameters() =
        (source as PsiSourceElement).psi?.children?.flatMap {
            it.safeAs<KtInitializerList>()?.initializersAsText().orEmpty()
        }.orEmpty()

    private fun KtInitializerList.initializersAsText() =
        initializers.firstIsInstanceOrNull<KtCallElement>()
            ?.getValueArgumentsInParentheses()
            ?.flatMap { it.childrenAsText() }
            .orEmpty()

    private fun ValueArgument.childrenAsText() = this.safeAs<KtValueArgument>()?.children?.map { it.text }.orEmpty()

    private data class ClassInfo(val superclasses: List<DRI>, val interfaces: List<DRI>, val docs: SourceSetDependent<DocumentationNode>){
        val supertypes: List<DRI>
            get() = (superclasses + interfaces).distinct()
    }

    private fun Visibility.toDokkaVisibility(): org.jetbrains.dokka.model.Visibility = when (this) {
        Visibilities.PUBLIC -> KotlinVisibility.Public
        Visibilities.PROTECTED -> KotlinVisibility.Protected
        Visibilities.INTERNAL -> KotlinVisibility.Internal
        Visibilities.PRIVATE -> KotlinVisibility.Private
        else -> KotlinVisibility.Public
    }

    private fun ConstantsEnumValue.fullEnumEntryName() =
        "${this.enumClassId.relativeClassName.asString()}.${this.enumEntryName.identifier}"

    private fun fallbackPackageName(): String =
        "[${sourceSet.displayName} root]"// TODO: error-prone, find a better way to do it
}

private fun DRI.withPackageFallbackTo(fallbackPackage: String): DRI {
    return if (packageName.isNullOrBlank()) {
        copy(packageName = fallbackPackage)
    } else {
        this
    }
}
