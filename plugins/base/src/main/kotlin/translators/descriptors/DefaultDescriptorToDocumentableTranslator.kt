package org.jetbrains.dokka.base.translators.descriptors

import com.intellij.psi.PsiNamedElement
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.DescriptorDocumentableSource
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.analysis.KotlinAnalysis
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.parsers.MarkdownParser
import org.jetbrains.dokka.base.translators.isDirectlyAnException
import org.jetbrains.dokka.base.translators.psi.parsers.JavadocParser
import org.jetbrains.dokka.base.translators.unquotedValue
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.sources.AsyncSourceToDocumentableTranslator
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.utilities.parallelMap
import org.jetbrains.dokka.utilities.parallelMapNotNull
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.isBuiltinExtensionFunctionalType
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.codegen.isJvmStaticInObjectOrClassOrInterface
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.core.getDirectlyOverriddenDeclarations
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
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
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.nio.file.Paths
import org.jetbrains.kotlin.resolve.constants.AnnotationValue as ConstantsAnnotationValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue as ConstantsArrayValue
import org.jetbrains.kotlin.resolve.constants.EnumValue as ConstantsEnumValue
import org.jetbrains.kotlin.resolve.constants.KClassValue as ConstantsKtClassValue

class DefaultDescriptorToDocumentableTranslator(
    context: DokkaContext
) : AsyncSourceToDocumentableTranslator {

    private val kotlinAnalysis: KotlinAnalysis = context.plugin<DokkaBase>().querySingle { kotlinAnalysis }

    override suspend fun invokeSuspending(sourceSet: DokkaSourceSet, context: DokkaContext): DModule {
        val (environment, facade) = kotlinAnalysis[sourceSet]
        val packageFragments = environment.getSourceFiles().asSequence()
            .map { it.packageFqName }
            .distinct()
            .mapNotNull { facade.resolveSession.getPackageFragment(it) }
            .toList()

        return DokkaDescriptorVisitor(sourceSet, kotlinAnalysis[sourceSet].facade, context.logger).run {
            packageFragments.mapNotNull { it.safeAs<PackageFragmentDescriptor>() }.parallelMap {
                visitPackageFragmentDescriptor(
                    it,
                    DRIWithPlatformInfo(DRI.topLevel, emptyMap())
                )
            }
        }.let {
            DModule(
                name = context.configuration.moduleName,
                packages = it,
                documentation = emptyMap(),
                expectPresentInSet = null,
                sourceSets = setOf(sourceSet)
            )
        }
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
) {
    private val javadocParser = JavadocParser(logger, resolutionFacade)

    private fun Collection<DeclarationDescriptor>.filterDescriptorsInSourceSet() = filter {
        it.toSourceElement.containingFile.toString().let { path ->
            path.isNotBlank() && sourceSet.sourceRoots.any { root ->
                Paths.get(path).startsWith(root.toPath())
            }
        }
    }

    private fun <T> T.toSourceSetDependent() = if (this != null) mapOf(sourceSet to this) else emptyMap()

    suspend fun visitPackageFragmentDescriptor(
        descriptor: PackageFragmentDescriptor,
        parent: DRIWithPlatformInfo
    ): DPackage {
        val name = descriptor.fqName.asString().takeUnless { it.isBlank() } ?: ""
        val driWithPlatform = DRI(packageName = name).withEmptyInfo()
        val scope = descriptor.getMemberScope()
        return coroutineScope {
            val descriptorsWithKind = scope.getDescriptorsWithKind(true)

            val functions = async { descriptorsWithKind.functions.visitFunctions(driWithPlatform) }
            val properties = async { descriptorsWithKind.properties.visitProperties(driWithPlatform) }
            val classlikes = async { descriptorsWithKind.classlikes.visitClasslikes(driWithPlatform) }
            val typealiases = async { descriptorsWithKind.typealiases.visitTypealiases(driWithPlatform) }

            DPackage(
                dri = driWithPlatform.dri,
                functions = functions.await(),
                properties = properties.await(),
                classlikes = classlikes.await(),
                typealiases = typealiases.await(),
                documentation = descriptor.resolveDescriptorData(),
                sourceSets = setOf(sourceSet)
            )
        }
    }

    private suspend fun visitClassDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DClasslike =
        when (descriptor.kind) {
            ClassKind.ENUM_CLASS -> enumDescriptor(descriptor, parent)
            ClassKind.OBJECT -> objectDescriptor(descriptor, parent)
            ClassKind.INTERFACE -> interfaceDescriptor(descriptor, parent)
            ClassKind.ANNOTATION_CLASS -> annotationDescriptor(descriptor, parent)
            else -> classDescriptor(descriptor, parent)
        }

    private suspend fun interfaceDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DInterface {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val isActual = descriptor.isActual
        val info = descriptor.resolveClassDescriptionData()

        return coroutineScope {
            val descriptorsWithKind = scope.getDescriptorsWithKind()

            val functions = async { descriptorsWithKind.functions.visitFunctions(driWithPlatform) }
            val properties = async { descriptorsWithKind.properties.visitProperties(driWithPlatform) }
            val classlikes = async { descriptorsWithKind.classlikes.visitClasslikes(driWithPlatform) }
            val generics = async { descriptor.declaredTypeParameters.parallelMap { it.toVariantTypeParameter() } }

            DInterface(
                dri = driWithPlatform.dri,
                name = descriptor.name.asString(),
                functions = functions.await(),
                properties = properties.await(),
                classlikes = classlikes.await(),
                sources = descriptor.createSources(),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
                supertypes = info.supertypes.toSourceSetDependent(),
                documentation = info.docs,
                generics = generics.await(),
                companion = descriptor.companion(driWithPlatform),
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                    descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                    ImplementedInterfaces(info.allImplementedInterfaces.toSourceSetDependent()),
                    info.exceptionsInSupertypes?.let { ExceptionInSupertypes(it.toSourceSetDependent()) },
                )
            )
        }
    }

    private suspend fun objectDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DObject {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val isActual = descriptor.isActual
        val info = descriptor.resolveClassDescriptionData()


        return coroutineScope {
            val descriptorsWithKind = scope.getDescriptorsWithKind()

            val functions = async { descriptorsWithKind.functions.visitFunctions(driWithPlatform) }
            val properties = async { descriptorsWithKind.properties.visitProperties(driWithPlatform) }
            val classlikes = async { descriptorsWithKind.classlikes.visitClasslikes(driWithPlatform) }

            DObject(
                dri = driWithPlatform.dri,
                name = descriptor.name.asString(),
                functions = functions.await(),
                properties = properties.await(),
                classlikes = classlikes.await(),
                sources = descriptor.createSources(),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
                supertypes = info.supertypes.toSourceSetDependent(),
                documentation = info.docs,
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                    descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                    ImplementedInterfaces(info.allImplementedInterfaces.toSourceSetDependent()),
                    info.exceptionsInSupertypes?.let { ExceptionInSupertypes(it.toSourceSetDependent()) },
                )
            )
        }


    }

    private suspend fun enumDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DEnum {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val isActual = descriptor.isActual
        val info = descriptor.resolveClassDescriptionData()

        return coroutineScope {
            val descriptorsWithKind = scope.getDescriptorsWithKind()

            val functions = async { descriptorsWithKind.functions.visitFunctions(driWithPlatform) }
            val properties = async { descriptorsWithKind.properties.visitProperties(driWithPlatform) }
            val classlikes = async { descriptorsWithKind.classlikes.visitClasslikes(driWithPlatform) }
            val constructors =
                async { descriptor.constructors.parallelMap { visitConstructorDescriptor(it, driWithPlatform) } }
            val entries = async { descriptorsWithKind.enumEntries.visitEnumEntries(driWithPlatform) }

            DEnum(
                dri = driWithPlatform.dri,
                name = descriptor.name.asString(),
                entries = entries.await(),
                constructors = constructors.await(),
                functions = functions.await(),
                properties = properties.await(),
                classlikes = classlikes.await(),
                sources = descriptor.createSources(),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
                supertypes = info.supertypes.toSourceSetDependent(),
                documentation = info.docs,
                companion = descriptor.companion(driWithPlatform),
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                    descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                    ImplementedInterfaces(info.allImplementedInterfaces.toSourceSetDependent())
                )
            )
        }
    }

    private suspend fun visitEnumEntryDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DEnumEntry {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect

        return coroutineScope {
            val descriptorsWithKind = scope.getDescriptorsWithKind()

            val functions = async { descriptorsWithKind.functions.visitFunctions(driWithPlatform) }
            val properties = async { descriptorsWithKind.properties.visitProperties(driWithPlatform) }
            val classlikes = async { descriptorsWithKind.classlikes.visitClasslikes(driWithPlatform) }

            DEnumEntry(
                dri = driWithPlatform.dri,
                name = descriptor.name.asString(),
                documentation = descriptor.resolveDescriptorData(),
                functions = functions.await(),
                properties = properties.await(),
                classlikes = classlikes.await(),
                sourceSets = setOf(sourceSet),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                extra = PropertyContainer.withAll(
                    descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                    descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                    ConstructorValues(descriptor.getAppliedConstructorParameters().toSourceSetDependent())
                )
            )
        }
    }

    private suspend fun annotationDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DAnnotation {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val isActual = descriptor.isActual

        return coroutineScope {
            val descriptorsWithKind = scope.getDescriptorsWithKind()

            val functions = async { descriptorsWithKind.functions.visitFunctions(driWithPlatform) }
            val properties = async { descriptorsWithKind.properties.visitProperties(driWithPlatform) }
            val classlikes = async { descriptorsWithKind.classlikes.visitClasslikes(driWithPlatform) }
            val generics = async { descriptor.declaredTypeParameters.parallelMap { it.toVariantTypeParameter() } }
            val constructors =
                async { descriptor.constructors.parallelMap { visitConstructorDescriptor(it, driWithPlatform) } }

            DAnnotation(
                dri = driWithPlatform.dri,
                name = descriptor.name.asString(),
                documentation = descriptor.resolveDescriptorData(),
                functions = functions.await(),
                properties = properties.await(),
                classlikes = classlikes.await(),
                expectPresentInSet = sourceSet.takeIf { isExpect },
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                    descriptor.getAnnotations().toSourceSetDependent().toAnnotations()
                ),
                companion = descriptor.companionObjectDescriptor?.let { objectDescriptor(it, driWithPlatform) },
                visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
                generics = generics.await(),
                constructors = constructors.await(),
                sources = descriptor.createSources()
            )
        }


    }

    private suspend fun classDescriptor(descriptor: ClassDescriptor, parent: DRIWithPlatformInfo): DClass {
        val driWithPlatform = parent.dri.withClass(descriptor.name.asString()).withEmptyInfo()
        val scope = descriptor.unsubstitutedMemberScope
        val isExpect = descriptor.isExpect
        val isActual = descriptor.isActual
        val info = descriptor.resolveClassDescriptionData()
        val actual = descriptor.createSources()

        return coroutineScope {
            val descriptorsWithKind = scope.getDescriptorsWithKind()

            val functions = async { descriptorsWithKind.functions.visitFunctions(driWithPlatform) }
            val properties = async { descriptorsWithKind.properties.visitProperties(driWithPlatform) }
            val classlikes = async { descriptorsWithKind.classlikes.visitClasslikes(driWithPlatform) }
            val generics = async { descriptor.declaredTypeParameters.parallelMap { it.toVariantTypeParameter() } }
            val constructors = async {
                descriptor.constructors.parallelMap {
                    visitConstructorDescriptor(
                        it,
                        if (it.isPrimary) DRIWithPlatformInfo(driWithPlatform.dri, actual)
                        else DRIWithPlatformInfo(driWithPlatform.dri, emptyMap())
                    )
                }
            }

            DClass(
                dri = driWithPlatform.dri,
                name = descriptor.name.asString(),
                constructors = constructors.await(),
                functions = functions.await(),
                properties = properties.await(),
                classlikes = classlikes.await(),
                sources = actual,
                expectPresentInSet = sourceSet.takeIf { isExpect },
                visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
                supertypes = info.supertypes.toSourceSetDependent(),
                generics = generics.await(),
                documentation = info.docs,
                modifier = descriptor.modifier().toSourceSetDependent(),
                companion = descriptor.companion(driWithPlatform),
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll<DClass>(
                    descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                    descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                    ImplementedInterfaces(info.allImplementedInterfaces.toSourceSetDependent()),
                    info.exceptionsInSupertypes?.let { ExceptionInSupertypes(it.toSourceSetDependent()) },
                )
            )
        }
    }

    private suspend fun visitPropertyDescriptor(
        originalDescriptor: PropertyDescriptor,
        parent: DRIWithPlatformInfo
    ): DProperty {
        val dri = parent.dri.copy(callable = Callable.from(originalDescriptor))
        val descriptor = originalDescriptor.getConcreteDescriptor()
        val inheritedFrom = descriptor.createDRI().let { (originalDri, _) -> originalDri.takeIf { it != dri } }
        val isExpect = descriptor.isExpect
        val isActual = descriptor.isActual

        val actual = originalDescriptor.createSources()

        return coroutineScope {
            val generics = async { descriptor.typeParameters.parallelMap { it.toVariantTypeParameter() } }

            DProperty(
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
                generics = generics.await(),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    listOfNotNull(
                        (descriptor.additionalExtras() + descriptor.getAnnotationsWithBackingField()
                            .toAdditionalExtras()).toSet().toSourceSetDependent().toAdditionalModifiers(),
                        (descriptor.getAnnotationsWithBackingField() + descriptor.fileLevelAnnotations()).toSourceSetDependent()
                            .toAnnotations(),
                        descriptor.getDefaultValue()?.let { DefaultValue(it) },
                        InheritedMember(inheritedFrom.toSourceSetDependent()),
                    )
                )
            )
        }
    }

    private fun CallableMemberDescriptor.createDRI(wasOverridenBy: DRI? = null): Pair<DRI, DRI?> =
        if (kind == CallableMemberDescriptor.Kind.DECLARATION || overriddenDescriptors.isEmpty())
            Pair(DRI.from(this), wasOverridenBy)
        else
            overriddenDescriptors.first().createDRI(DRI.from(this))

    private suspend fun visitFunctionDescriptor(
        originalDescriptor: FunctionDescriptor,
        parent: DRIWithPlatformInfo
    ): DFunction {
        val (dri, inheritedFrom) = originalDescriptor.createDRI()
        val descriptor = originalDescriptor.getConcreteDescriptor()
        val isExpect = descriptor.isExpect
        val isActual = descriptor.isActual

        val actual = originalDescriptor.createSources()
        return coroutineScope {
            val generics = async { descriptor.typeParameters.parallelMap { it.toVariantTypeParameter() } }

            DFunction(
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
                generics = generics.await(),
                documentation = descriptor.takeIf { it.kind != CallableMemberDescriptor.Kind.SYNTHESIZED }
                    ?.resolveDescriptorData() ?: emptyMap(),
                modifier = descriptor.modifier().toSourceSetDependent(),
                type = descriptor.returnType!!.toBound(),
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll(
                    InheritedMember(inheritedFrom.toSourceSetDependent()),
                    descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                    (descriptor.getAnnotations() + descriptor.fileLevelAnnotations()).toSourceSetDependent()
                        .toAnnotations(),
                    ObviousMember.takeIf { descriptor.isObvious },
                )
            )
        }
    }

    suspend fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, parent: DRIWithPlatformInfo): DFunction {
        val name = descriptor.constructedClass.name.toString()
        val dri = parent.dri.copy(callable = Callable.from(descriptor, name))
        val actual = descriptor.createSources()
        val isExpect = descriptor.isExpect
        val isActual = descriptor.isActual

        return coroutineScope {
            val generics = async { descriptor.typeParameters.parallelMap { it.toVariantTypeParameter() } }

            DFunction(
                dri = dri,
                name = name,
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
                generics = generics.await(),
                sourceSets = setOf(sourceSet),
                isExpectActual = (isExpect || isActual),
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
    }

    private suspend fun visitReceiverParameterDescriptor(
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

    private suspend fun visitPropertyAccessorDescriptor(
        descriptor: PropertyAccessorDescriptor,
        propertyDescriptor: PropertyDescriptor,
        parent: DRI
    ): DFunction {
        val dri = parent.copy(callable = Callable.from(descriptor))
        val isGetter = descriptor is PropertyGetterDescriptor
        val isExpect = descriptor.isExpect
        val isActual = descriptor.isActual

        suspend fun PropertyDescriptor.asParameter(parent: DRI) =
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
            "$modifier${rawName.capitalize()}"
        }

        val parameters =
            if (isGetter) {
                emptyList()
            } else {
                listOf(propertyDescriptor.asParameter(dri))
            }

        return coroutineScope {
            val generics = async { descriptor.typeParameters.parallelMap { it.toVariantTypeParameter() } }

            DFunction(
                dri,
                name,
                isConstructor = false,
                parameters = parameters,
                visibility = descriptor.visibility.toDokkaVisibility().toSourceSetDependent(),
                documentation = descriptor.resolveDescriptorData(),
                type = descriptor.returnType!!.toBound(),
                generics = generics.await(),
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
                isExpectActual = (isExpect || isActual),
                extra = PropertyContainer.withAll<DFunction>(
                    descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                    descriptor.getAnnotations().toSourceSetDependent().toAnnotations()
                )
            )
        }
    }

    private suspend fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, parent: DRIWithPlatformInfo?) =
        with(descriptor) {
            coroutineScope {
                val generics = async { descriptor.declaredTypeParameters.parallelMap { it.toVariantTypeParameter() } }
                val info = buildAncestryInformation(listOf(underlyingType)).sortedBy { it.level }

                DTypeAlias(
                    dri = DRI.from(this@with),
                    name = name.asString(),
                    type = defaultType.toBound(),
                    expectPresentInSet = null,
                    underlyingType = underlyingType.toBound().toSourceSetDependent(),
                    visibility = visibility.toDokkaVisibility().toSourceSetDependent(),
                    documentation = resolveDescriptorData(),
                    sourceSets = setOf(sourceSet),
                    generics = generics.await(),
                    extra = PropertyContainer.withAll(
                        descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                        info.exceptionsInSupertypes()?.takeIf { it.isNotEmpty() }
                            ?.let { ExceptionInSupertypes(it.toSourceSetDependent()) },
                    )
                )
            }
        }

    private suspend fun parameter(index: Int, descriptor: ValueParameterDescriptor, parent: DRIWithPlatformInfo) =
        DParameter(
            dri = parent.dri.copy(target = PointingToCallableParameters(index)),
            name = descriptor.name.asString(),
            type = descriptor.varargElementType?.toBound() ?: descriptor.type.toBound(),
            expectPresentInSet = null,
            documentation = descriptor.resolveDescriptorData(),
            sourceSets = setOf(sourceSet),
            extra = PropertyContainer.withAll(listOfNotNull(
                descriptor.additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                descriptor.getAnnotations().toSourceSetDependent().toAnnotations(),
                descriptor.getDefaultValue()?.let { DefaultValue(it) }
            ))
        )

    private data class DescriptorsWithKind(
        val functions: List<FunctionDescriptor>,
        val properties: List<PropertyDescriptor>,
        val classlikes: List<ClassDescriptor>,
        val typealiases: List<TypeAliasDescriptor>,
        val enumEntries: List<ClassDescriptor>
    )

    private suspend fun MemberScope.getDescriptorsWithKind(shouldFilter: Boolean = false): DescriptorsWithKind {
        val descriptors = getContributedDescriptors { true }.let {
            if (shouldFilter) it.filterDescriptorsInSourceSet() else it
        }

        class EnumEntryDescriptor

        val groupedDescriptors = descriptors.groupBy {
            when {
                it is FunctionDescriptor -> FunctionDescriptor::class
                it is PropertyDescriptor -> PropertyDescriptor::class
                it is ClassDescriptor && it.kind != ClassKind.ENUM_ENTRY -> ClassDescriptor::class
                it is TypeAliasDescriptor -> TypeAliasDescriptor::class
                it is ClassDescriptor && it.kind == ClassKind.ENUM_ENTRY -> EnumEntryDescriptor::class
                else -> IllegalStateException::class
            }
        }

        return DescriptorsWithKind(
            (groupedDescriptors[FunctionDescriptor::class] ?: emptyList()) as List<FunctionDescriptor>,
            (groupedDescriptors[PropertyDescriptor::class] ?: emptyList()) as List<PropertyDescriptor>,
            (groupedDescriptors[ClassDescriptor::class] ?: emptyList()) as List<ClassDescriptor>,
            (groupedDescriptors[TypeAliasDescriptor::class] ?: emptyList()) as List<TypeAliasDescriptor>,
            (groupedDescriptors[EnumEntryDescriptor::class] ?: emptyList()) as List<ClassDescriptor>
        )
    }

    private suspend fun List<FunctionDescriptor>.visitFunctions(parent: DRIWithPlatformInfo): List<DFunction> =
        coroutineScope { parallelMap { visitFunctionDescriptor(it, parent) } }

    private suspend fun List<PropertyDescriptor>.visitProperties(parent: DRIWithPlatformInfo): List<DProperty> =
        coroutineScope { parallelMap { visitPropertyDescriptor(it, parent) } }

    private suspend fun List<ClassDescriptor>.visitClasslikes(parent: DRIWithPlatformInfo): List<DClasslike> =
        coroutineScope { parallelMap { visitClassDescriptor(it, parent) } }

    private suspend fun List<TypeAliasDescriptor>.visitTypealiases(parent: DRIWithPlatformInfo): List<DTypeAlias> =
        coroutineScope { parallelMap { visitTypeAliasDescriptor(it, parent) } }

    private suspend fun List<ClassDescriptor>.visitEnumEntries(parent: DRIWithPlatformInfo): List<DEnumEntry> =
        coroutineScope { parallelMap { visitEnumEntryDescriptor(it, parent) } }

    private fun DeclarationDescriptor.resolveDescriptorData(): SourceSetDependent<DocumentationNode> =
        getDocumentation()?.toSourceSetDependent() ?: emptyMap()


    private suspend fun toTypeConstructor(kt: KotlinType) =
        GenericTypeConstructor(
            DRI.from(kt.constructor.declarationDescriptor as DeclarationDescriptor),
            kt.arguments.map { it.toProjection() })


    private tailrec suspend fun buildAncestryInformation(
        supertypes: Collection<KotlinType>,
        level: Int = 0,
        ancestryInformation: Set<AncestryLevel> = emptySet()
    ): Set<AncestryLevel> {
        if (supertypes.isEmpty()) return ancestryInformation

        val (interfaces, superclass) = supertypes
            .partition {
                val declaration = it.constructor.declarationDescriptor
                val descriptor = declaration as? ClassDescriptor
                    ?: (declaration as? TypeAliasDescriptor)?.underlyingType?.constructor?.declarationDescriptor as? ClassDescriptor
                descriptor?.kind == ClassKind.INTERFACE
            }

        val updated = coroutineScope {
            ancestryInformation + AncestryLevel(
                level,
                superclass.parallelMap(::toTypeConstructor).singleOrNull(),
                interfaces.parallelMap(::toTypeConstructor)
            )
        }

        return buildAncestryInformation(
            supertypes = supertypes.flatMap { it.immediateSupertypes() },
            level = level + 1,
            ancestryInformation = updated
        )
    }

    private suspend fun ClassDescriptor.resolveClassDescriptionData(): ClassInfo {
        return coroutineScope {
            ClassInfo(
                buildAncestryInformation(this@resolveClassDescriptionData.typeConstructor.supertypes.filterNot { it.isAnyOrNullableAny() }).sortedBy { it.level },
                resolveDescriptorData()
            )
        }
    }

    private suspend fun TypeParameterDescriptor.toVariantTypeParameter() =
        DTypeParameter(
            variantTypeParameter(
                TypeParameter(DRI.from(this), name.identifier, annotations.getPresentableName())
            ),
            resolveDescriptorData(),
            null,
            upperBounds.map { it.toBound() },
            setOf(sourceSet),
            extra = PropertyContainer.withAll(
                additionalExtras().toSourceSetDependent().toAdditionalModifiers(),
                getAnnotations().toSourceSetDependent().toAnnotations()
            )
        )

    private suspend fun org.jetbrains.kotlin.descriptors.annotations.Annotations.getPresentableName(): String? =
        mapNotNull { it.toAnnotation() }.singleOrNull { it.dri.classNames == "ParameterName" }?.params?.get("name")
            .safeAs<StringValue>()?.value?.let { unquotedValue(it) }

    private suspend fun KotlinType.toBound(): Bound {
        suspend fun <T : AnnotationTarget> annotations(): PropertyContainer<T> =
            getAnnotations().takeIf { it.isNotEmpty() }?.let { annotations ->
                PropertyContainer.withAll(annotations.toSourceSetDependent().toAnnotations())
            } ?: PropertyContainer.empty()

        return when (this) {
            is DynamicType -> Dynamic
            is AbbreviatedType -> TypeAliased(
                abbreviation.toBound(),
                expandedType.toBound()
            )
            else -> when (val ctor = constructor.declarationDescriptor) {
                is TypeParameterDescriptor -> TypeParameter(
                    dri = DRI.from(ctor),
                    name = ctor.name.asString(),
                    presentableName = annotations.getPresentableName(),
                    extra = annotations()
                )
                is FunctionClassDescriptor -> FunctionalTypeConstructor(
                    DRI.from(ctor),
                    arguments.map { it.toProjection() },
                    isExtensionFunction = isExtensionFunctionType || isBuiltinExtensionFunctionalType,
                    isSuspendable = isSuspendFunctionTypeOrSubtype,
                    presentableName = annotations.getPresentableName(),
                    extra = annotations()
                )
                else -> GenericTypeConstructor(
                    DRI.from(ctor!!), // TODO: remove '!!'
                    arguments.map { it.toProjection() },
                    annotations.getPresentableName(),
                    extra = annotations()
                )
            }.let {
                if (isMarkedNullable) Nullable(it) else it
            }
        }
    }

    private suspend fun TypeProjection.toProjection(): Projection =
        if (isStarProjection) Star else formPossiblyVariant()

    private suspend fun TypeProjection.formPossiblyVariant(): Projection =
        type.toBound().wrapWithVariance(projectionKind)

    private fun TypeParameterDescriptor.variantTypeParameter(type: TypeParameter) =
        type.wrapWithVariance(variance)

    private fun <T : Bound> T.wrapWithVariance(variance: org.jetbrains.kotlin.types.Variance) =
        when (variance) {
            org.jetbrains.kotlin.types.Variance.INVARIANT -> Invariance(this)
            org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> Contravariance(this)
            org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> Covariance(this)
        }

    private fun DeclarationDescriptor.getDocumentation() = (findKDoc()?.let {
        MarkdownParser.parseFromKDocTag(
            kDocTag = it,
            externalDri = { link: String ->
                try {
                    resolveKDocLink(
                        context = resolutionFacade.resolveSession.bindingContext,
                        resolutionFacade = resolutionFacade,
                        fromDescriptor = this,
                        fromSubjectOfTag = null,
                        qualifiedName = link.split('.')
                    ).firstOrNull()?.let { DRI.from(it) }
                } catch (e1: IllegalArgumentException) {
                    logger.warn("Couldn't resolve link for $link")
                    null
                }
            },
            kdocLocation = toSourceElement?.containingFile?.name?.let {
                val fqName = fqNameOrNull()?.asString()
                if (fqName != null) "$it/$fqName"
                else it
            }
        )
    } ?: getJavaDocs())?.takeIf { it.children.isNotEmpty() }

    private fun DeclarationDescriptor.getJavaDocs() = (this as? CallableDescriptor)
        ?.overriddenDescriptors
        ?.mapNotNull { it.findPsi() as? PsiNamedElement }
        ?.firstNotNullResult { javadocParser.parseDocumentation(it) }

    private suspend fun ClassDescriptor.companion(dri: DRIWithPlatformInfo): DObject? = companionObjectDescriptor?.let {
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
        ExtraModifiers.KotlinOnlyModifiers.Value.takeIf { isValue },
        ExtraModifiers.KotlinOnlyModifiers.External.takeIf { isExternal },
        ExtraModifiers.KotlinOnlyModifiers.Inner.takeIf { isInner },
        ExtraModifiers.KotlinOnlyModifiers.Data.takeIf { isData },
        ExtraModifiers.KotlinOnlyModifiers.Fun.takeIf { isFun },
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

    private suspend fun Annotated.getAnnotations() = annotations.parallelMapNotNull { it.toAnnotation() }

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
        else -> StringValue(unquotedValue(toString()))
    }

    private fun AnnotationDescriptor.toAnnotation(scope: Annotations.AnnotationScope = Annotations.AnnotationScope.DIRECT): Annotations.Annotation =
        Annotations.Annotation(
            DRI.from(annotationClass as DeclarationDescriptor),
            allValueArguments.map { it.key.asString() to it.value.toValue() }.filter {
                it.second != null
            }.toMap() as Map<String, AnnotationParameterValue>,
            mustBeDocumented(),
            scope
        )

    private fun AnnotationDescriptor.mustBeDocumented(): Boolean =
        if (source.toString() == "NO_SOURCE") false
        else annotationClass?.annotations?.hasAnnotation(FqName("kotlin.annotation.MustBeDocumented")) ?: false

    private suspend fun PropertyDescriptor.getAnnotationsWithBackingField(): List<Annotations.Annotation> =
        getAnnotations() + (backingField?.getAnnotations() ?: emptyList())

    private fun List<Annotations.Annotation>.toAdditionalExtras() = mapNotNull {
        try {
            ExtraModifiers.valueOf(it.dri.classNames?.toLowerCase() ?: "")
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun <T : CallableMemberDescriptor> T.getConcreteDescriptor(): T =
        if (kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) this
        else overriddenDescriptors.first().getConcreteDescriptor() as T

    private fun ValueParameterDescriptor.getDefaultValue(): String? =
        (source as? KotlinSourceElement)?.psi?.children?.find { it is KtExpression }?.text

    private suspend fun PropertyDescriptor.getDefaultValue(): String? =
        (source as? KotlinSourceElement)?.psi?.children?.find { it is KtConstantExpression }?.text

    private suspend fun ClassDescriptor.getAppliedConstructorParameters() =
        (source as PsiSourceElement).psi?.children?.flatMap {
            it.safeAs<KtInitializerList>()?.initializersAsText().orEmpty()
        }.orEmpty()

    private suspend fun KtInitializerList.initializersAsText() =
        initializers.firstIsInstanceOrNull<KtCallElement>()
            ?.getValueArgumentsInParentheses()
            ?.flatMap { it.childrenAsText() }
            .orEmpty()

    private fun ValueArgument.childrenAsText() =
        this.safeAs<KtValueArgument>()?.children?.map { it.text }.orEmpty()

    private data class ClassInfo(val ancestry: List<AncestryLevel>, val docs: SourceSetDependent<DocumentationNode>) {
        val supertypes: List<TypeConstructorWithKind>
            get() = ancestry.firstOrNull { it.level == 0 }?.let {
                listOfNotNull(it.superclass?.let {
                    TypeConstructorWithKind(
                        it,
                        KotlinClassKindTypes.CLASS
                    )
                }) + it.interfaces.map { TypeConstructorWithKind(it, KotlinClassKindTypes.INTERFACE) }
            }.orEmpty()

        val allImplementedInterfaces: List<TypeConstructor>
            get() = ancestry.flatMap { it.interfaces }.distinct()

        val exceptionsInSupertypes: List<TypeConstructor>?
            get() = ancestry.exceptionsInSupertypes()
    }

    private fun DescriptorVisibility.toDokkaVisibility(): org.jetbrains.dokka.model.Visibility = when (this.delegate) {
        Visibilities.Public -> KotlinVisibility.Public
        Visibilities.Protected -> KotlinVisibility.Protected
        Visibilities.Internal -> KotlinVisibility.Internal
        Visibilities.Private -> KotlinVisibility.Private
        else -> KotlinVisibility.Public
    }

    private fun ConstantsEnumValue.fullEnumEntryName() =
        "${this.enumClassId.relativeClassName.asString()}.${this.enumEntryName.identifier}"

    private fun DeclarationDescriptorWithSource.ktFile(): KtFile? =
        (source.containingFile as? PsiSourceFile)?.psiFile as? KtFile

    private suspend fun DeclarationDescriptorWithSource.fileLevelAnnotations() = ktFile()
        ?.let { file -> resolutionFacade.resolveSession.getFileAnnotations(file) }
        ?.toList()
        ?.parallelMap { it.toAnnotation(scope = Annotations.AnnotationScope.FILE) }
        .orEmpty()

    private val FunctionDescriptor.isObvious: Boolean
        get() = kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE ||
                kind == CallableMemberDescriptor.Kind.SYNTHESIZED ||
                containingDeclaration.fqNameOrNull()?.asString()
                    ?.let { it == "kotlin.Any" || it == "kotlin.Enum" || it == "java.lang.Enum" || it == "java.lang.Object" } == true
}

private data class AncestryLevel(
    val level: Int,
    val superclass: TypeConstructor?,
    val interfaces: List<TypeConstructor>
)

private fun List<AncestryLevel>.exceptionsInSupertypes(): List<TypeConstructor>? =
    mapNotNull { it.superclass }.filter { type -> type.dri.isDirectlyAnException() }.takeIf { it.isNotEmpty() }
