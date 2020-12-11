package org.jetbrains.dokka.kotlinAsJava.converters

import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.Nullable
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import java.lang.IllegalStateException

private val DPackage.jvmNames: SourceSetDependent<Map<DocumentableSource, String>>?
    get() = extra[FileAnnotations]
        ?.content
        ?.mapValues { (_, fileAnnotations) ->
            fileAnnotations
                .asMap()
                .mapNotNull { (source, annotations) ->
                    annotations
                        .firstOrNull { annotation -> annotation.dri.classNames == "JvmName" }
                        ?.let { annotation -> annotation.params["name"] as? StringValue }
                        ?.let { value -> source to value.unquotedValue }
                }
                .toMap()
        }

private fun <T : WithSources> List<T>.groupedByLocation(
    jvmNames: SourceSetDependent<Map<DocumentableSource, String>>?
): Map<String, List<T>> {
    return this
        .map { withSources -> withSources.sources to withSources }
        .groupBy(
            { (location, _) ->
                val (sourceSet, source) = location.entries.first()
               jvmNames?.get(sourceSet)?.filterKeys { it.hasSamePath(source) }?.values?.firstOrNull()
                    ?: source.path.split("/").last().split(".").first() + "Kt"
            },
            { (_ , withSources) ->
                withSources
            }
        )
}

private val WithExtraProperties<out Documentable>.isJvmSynthetic: Boolean
    get() = hasAnnotation("JvmSynthetic")?.any { (_, value) -> value } == true

private fun <T : WithExtraProperties<out Documentable>> List<T>.filterNotSynthetic() = filterNot { it.isJvmSynthetic }

private val DProperty.isJvmField: Boolean
    get() = hasAnnotation("JvmField")?.any { (_, value) -> value } == true

private val DProperty.isConst: Boolean
    get() = hasAdditionalModifier(ExtraModifiers.KotlinOnlyModifiers.Const)?.any { (_, value) -> value } == true

internal fun DPackage.asJava(): DPackage {
    @Suppress("UNCHECKED_CAST")
    val syntheticClasses = ((properties + functions) as List<WithSources>)
        .groupedByLocation(jvmNames)
        .map { (syntheticClassName, nodes) ->
            DClass(
                dri = dri.withClass(syntheticClassName),
                name = syntheticClassName,
                properties = nodes
                    .filterIsInstance<DProperty>()
                    .filter { (it.isJvmField || it.isConst) && !it.isJvmSynthetic }
                    .map { it.asJava(true, syntheticClassName) },
                constructors = emptyList(),
                functions = (
                        nodes
                            .filterIsInstance<DProperty>()
                            .filterNot { it.isConst || it.isJvmField || it.isJvmSynthetic }
                            .flatMap { it.javaAccessors(true, syntheticClassName) } +
                                nodes
                                    .filterIsInstance<DFunction>()
                                    .filterNotSynthetic()
                                    .flatMap { it.asJava(syntheticClassName, true) }),
                classlikes = emptyList(),
                sources = emptyMap(),
                expectPresentInSet = null,
                visibility = sourceSets.map {
                    it to JavaVisibility.Public
                }.toMap(),
                companion = null,
                generics = emptyList(),
                supertypes = emptyMap(),
                documentation = emptyMap(),
                modifier = sourceSets.map { it to JavaModifier.Final }.toMap(),
                sourceSets = sourceSets,
                isExpectActual = false,
                extra = PropertyContainer.empty()
            )
        }

    return copy(
        functions = emptyList(),
        properties = emptyList(),
        classlikes = classlikes.map { it.asJava() } + syntheticClasses,
        typealiases = emptyList()
    )
}

internal fun DProperty.asJava(isTopLevel: Boolean = false, relocateToClass: String? = null): DProperty {
    val fromCompanionObject = extra[FromCompanionObject]
    val newClassName = relocateToClass ?: (fromCompanionObject?.container as? DClasslike)?.name
    return copy(
        dri = if (newClassName == null) dri else dri.copy(classNames = newClassName),
        modifier = javaModifierFromSetter(),
        visibility = if (fromCompanionObject == null && !isTopLevel && !isConst) {
            visibility.mapValues { it.value.propertyVisibilityAsJava() }
        } else {
            visibility.mapValues { JavaVisibility.Public }
        },
        type = type.asJava(), // TODO: check
        setter = null,
        getter = null, // Removing getters and setters as they will be available as functions
        extra = if (isTopLevel || fromCompanionObject != null) extra +
                extra.mergeAdditionalModifiers(
                    sourceSets.map {
                        it to setOf(ExtraModifiers.JavaOnlyModifiers.Static)
                    }.toMap()
                )
        else extra
    )
}

internal fun DProperty.javaModifierFromSetter() =
    modifier.mapValues {
        when {
            it.value is JavaModifier -> it.value
            setter == null -> JavaModifier.Final
            else -> JavaModifier.Empty
        }
    }

fun <T : Documentable> WithExtraProperties<T>.jvmName(): String? =
    extra[Annotations]
        ?.content
        ?.asSequence()
        ?.mapNotNull { (_, annotations) ->
            annotations.firstOrNull { it.dri.classNames == "JvmName" }
        }
        ?.mapNotNull { (it.params["name"] as? StringValue)?.unquotedValue }
        ?.firstOrNull()

// TODO : update callable names
internal fun DProperty.javaAccessors(isTopLevel: Boolean = false, relocateToClass: String? = null): List<DFunction> =
    listOfNotNull(
        getter
            ?.copy(
            dri = dri.copy(
                classNames = if (relocateToClass.isNullOrBlank()) dri.classNames else "${dri.classNames}.$relocateToClass",
            ),
            name = getter!!.jvmName() ?: "get" + name.capitalize(),
            modifier = javaModifierFromSetter(),
            visibility = visibility.mapValues { JavaVisibility.Public },
            type = type.asJava(), // TODO: check
            extra = if (isTopLevel) getter!!.extra +
                    getter!!.extra.mergeAdditionalModifiers(
                        sourceSets.map {
                            it to setOf(ExtraModifiers.JavaOnlyModifiers.Static)
                        }.toMap()
                    )
            else getter!!.extra
        ),
        setter?.copy(
            dri = dri.copy(
                classNames = if (relocateToClass.isNullOrBlank()) dri.classNames else "${dri.classNames}.$relocateToClass",
            ),
            name = setter!!.jvmName() ?: "set" + name.capitalize(),
            modifier = javaModifierFromSetter(),
            visibility = visibility.mapValues { JavaVisibility.Public },
            parameters = setter!!.parameters.map { it.asJava() },
            extra = if (isTopLevel) setter!!.extra + setter!!.extra.mergeAdditionalModifiers(
                sourceSets.map {
                    it to setOf(ExtraModifiers.JavaOnlyModifiers.Static)
                }.toMap()
            )
            else setter!!.extra
        )
    ).filterNotSynthetic()

// TODO : check is callable is OK
private fun DFunction.asJava(parameters: List<DParameter> = this.parameters, newName: String?, isTopLevel: Boolean): DFunction {
    val fromCompanionObject = extra[FromCompanionObject]
    val newClassName = (fromCompanionObject?.container as? DClasslike)?.name
    val jvmName = jvmName()
    return copy(
        dri = if (newClassName == null) {
            if (jvmName == null) {
                dri
            } else {
                dri.copy(callable = dri.callable?.copy(name = jvmName))
            }
        } else {
            if (jvmName == null) {
                dri.copy(classNames = newClassName)
            } else {
                dri.copy(
                    classNames = newClassName,
                    callable = dri.callable?.copy(name = jvmName)
                )
            }
        },
        name = newName ?: jvmName ?: name,
        type = type.asJava(),
        modifier = if (modifier.all { (_, v) -> v is KotlinModifier.Final } && isConstructor)
            sourceSets.map { it to JavaModifier.Empty }.toMap()
        else sourceSets.map { it to modifier.values.first() }.toMap(),
        visibility = if (fromCompanionObject == null) {
            visibility
        } else {
            visibility.mapValues { JavaVisibility.Public }
        },
        parameters = listOfNotNull(receiver?.asJava()) + parameters.map { it.asJava() },
        receiver = null,
        extra = if (isTopLevel || fromCompanionObject != null) {
            extra + extra.mergeAdditionalModifiers(
                sourceSets.map {
                    it to setOf(ExtraModifiers.JavaOnlyModifiers.Static)
                }.toMap()
            )
        } else {
            extra
        }
    )
}

private inline fun <T> MutableList<T>.removeLastIf(predicate: (T) -> Boolean): Boolean {
    val lastIndex = indexOfLast(predicate)
    return if (lastIndex < 0) {
        false
    } else {
        removeAt(lastIndex)
        true
    }
}

internal fun DFunction.asJava(containingClassName: String, isTopLevel: Boolean = false): List<DFunction> {
    val newName = if (isConstructor) containingClassName else null
    val functions = mutableListOf<DFunction>()
    // Function with all parameters is always there
    functions.add(asJava(parameters, newName, isTopLevel))
    if (hasAnnotation("JvmOverloads")?.entries?.first()?.value == true) {
        // Add generated overloads
        val parameters = this.parameters.toMutableList()
        while (parameters.removeLastIf { it.hasDefaultValue }) {
            functions.add(asJava(parameters, newName, isTopLevel))
        }
    }
    return functions.toList()
}

internal fun DClasslike.asJava(): DClasslike = when (this) {
    is DClass -> asJava()
    is DEnum -> asJava()
    is DAnnotation -> asJava()
    is DObject -> asJava()
    is DInterface -> asJava()
    else -> throw IllegalArgumentException("$this shouldn't be here")
}

private val WithVisibility.hasJavaVisibility: Boolean
    get() = visibility.any { (_, v) -> v is JavaVisibility }

private fun DClasslike.javaFunctions(): List<DFunction> {
    val companionFunctions = if (this is WithCompanion) {
        companion
            ?.functions
            ?.filter { it.isJavaStatic }
            ?.map { it.copy(extra = it.extra + FromCompanionObject(this)) }
            .orEmpty()
    } else {
        emptyList()
    }
    return (functions +
            properties
                .filter { it.hasJavaVisibility || (!it.isConst && !it.isJvmField && !it.isJvmSynthetic) }
                .flatMap { listOf(it.getter, it.setter) } +
            companionFunctions)
        .filterNotNull()
        .filterNotSynthetic()
}

private fun DClasslike.javaProperties(): List<DProperty> {
    val companionProperties = if (this is WithCompanion) {
        companion
            ?.properties
            ?.filter { it.isConst }
            ?.map { it.copy(extra = it.extra + FromCompanionObject(this)) }
            .orEmpty()
    } else {
        emptyList()
    }
    return (properties
        .filter { it.hasJavaVisibility || ((it.isConst || it.isJvmField) && !it.isJvmSynthetic) } +
            companionProperties)
}

private fun WithScope.classLikesAsJava(): List<DClasslike> = classlikes.map {
    if (this is WithCompanion && it is DObject && it.dri == companion?.dri) {
        it.asJava(true)
    } else {
        it.asJava()
    }
}

private fun <T> T.javaName(): String? where T : DClasslike, T : WithExtraProperties<out Documentable> =
    jvmName() ?: name

private fun <T> T.javaDri(): DRI where T : DClasslike, T : WithExtraProperties<out Documentable> =
    jvmName()?.let { name -> dri.copy(classNames = name) } ?: dri

private val DFunction.isJavaStatic: Boolean
    get() = hasAdditionalModifier(ExtraModifiers.JavaOnlyModifiers.Static)?.any { (_, value) -> value } == true

internal fun DClass.asJava(): DClass = copy(
    name = javaName()!!,
    dri = javaDri(),
    constructors = constructors.flatMap { it.asJava(name) },
    functions = javaFunctions().flatMap { it.asJava(name)},
    properties = javaProperties().map { it.asJava() },
    classlikes = classLikesAsJava(),
    generics = generics.map { it.asJava() },
    companion = companion?.asJava(true),
    supertypes = supertypes.mapValues { it.value.map { it.asJava() } },
    modifier = if (modifier.all { (_, v) -> v is KotlinModifier.Empty }) sourceSets.map { it to JavaModifier.Final }
        .toMap()
    else sourceSets.map { it to modifier.values.first() }.toMap()
)

private fun DTypeParameter.asJava(): DTypeParameter = copy(
    variantTypeParameter = variantTypeParameter.withDri(dri.possiblyAsJava()),
    bounds = bounds.map { it.asJava() }
)

private fun Projection.asJava(): Projection = when(this) {
    is Star -> Star
    is Covariance<*> -> copy(inner.asJava())
    is Contravariance<*> -> copy(inner.asJava())
    is Invariance<*> -> copy(inner.asJava())
    is Bound -> asJava()
}

private fun Bound.asJava(): Bound = when(this) {
    is TypeParameter -> copy(dri.possiblyAsJava())
    is GenericTypeConstructor -> copy(
        dri = dri.possiblyAsJava(),
        projections = projections.map { it.asJava() }
    )
    is FunctionalTypeConstructor -> copy(
        dri = dri.possiblyAsJava(),
        projections = projections.map { it.asJava() }
    )
    is TypeAliased -> copy(
        typeAlias = typeAlias.asJava(),
        inner = inner.asJava()
    )
    is Nullable -> copy(inner.asJava())
    is PrimitiveJavaType -> this
    is Void -> this
    is JavaObject -> this
    is Dynamic -> this
    is UnresolvedBound -> this
}

internal fun DEnum.asJava(): DEnum = copy(
    name = javaName()!!,
    dri = javaDri(),
    constructors = constructors.flatMap { it.asJava(name) },
    functions = javaFunctions().flatMap { it.asJava(name)},
    properties = javaProperties().map { it.asJava() },
    classlikes = classLikesAsJava(),
    companion = companion?.asJava(true),
    supertypes = supertypes.mapValues { it.value.map { it.asJava() } }
//    , entries = entries.map { it.asJava() }
)

internal fun DObject.asJava(isCompanion: Boolean = false): DObject = copy(
    name = javaName(),
    dri = javaDri(),
    functions = javaFunctions()
        .filter { !isCompanion || !it.isJavaStatic }
        .flatMap { it.asJava(name.orEmpty()) },
    properties = javaProperties()
        .filter { !isCompanion || !it.isConst }
        .map { it.asJava() } +
            DProperty(
                name = "INSTANCE",
                modifier = sourceSets.map { it to JavaModifier.Final }.toMap(),
                dri = dri.copy(callable = Callable("INSTANCE", null, emptyList())),
                documentation = emptyMap(),
                sources = emptyMap(),
                visibility = sourceSets.map {
                    it to JavaVisibility.Public
                }.toMap(),
                type = GenericTypeConstructor(dri, emptyList()),
                setter = null,
                getter = null,
                sourceSets = sourceSets,
                receiver = null,
                generics = emptyList(),
                expectPresentInSet = expectPresentInSet,
                isExpectActual = false,
                extra = PropertyContainer.withAll(sourceSets.map {
                    mapOf(it to setOf(ExtraModifiers.JavaOnlyModifiers.Static)).toAdditionalModifiers()
                })
            ),
    classlikes = classLikesAsJava(),
    supertypes = supertypes.mapValues { it.value.map { it.asJava() } }
)

internal fun DInterface.asJava(): DInterface = copy(
    name = javaName()!!,
    dri = javaDri(),
    functions = (functions +
            properties.filterNot { it.hasJavaVisibility }.flatMap { listOf(it.getter, it.setter) })
        .filterNotNull()
        .filterNotSynthetic()
        .flatMap { it.asJava(name) },
    properties = (properties.filter { it.hasJavaVisibility }
            + companion?.properties?.filter { it.isConst }.orEmpty()),
    classlikes = classLikesAsJava(), // TODO: public static final class DefaultImpls with impls for methods
    generics = generics.map { it.asJava() },
    companion = companion?.asJava(true),
    supertypes = supertypes.mapValues { it.value.map { it.asJava() } }
)

internal fun DAnnotation.asJava(): DAnnotation = copy(
    name = javaName()!!,
    dri = javaDri(),
    properties = properties.map { it.asJava() },
    constructors = emptyList(),
    classlikes = classLikesAsJava()
) // TODO investigate if annotation class can have methods and properties not from constructor

internal fun DParameter.asJava(): DParameter = copy(
    type = type.asJava(),
    name = if (name.isNullOrBlank()) "\$self" else name
)

internal fun Visibility.propertyVisibilityAsJava(): Visibility = (this as? JavaVisibility) ?: JavaVisibility.Private

internal fun String.getAsPrimitive(): JvmPrimitiveType? = org.jetbrains.kotlin.builtins.PrimitiveType.values()
    .find { it.typeFqName.asString() == this }
    ?.let { JvmPrimitiveType.get(it) }

private fun DRI.partialFqName() = packageName?.let { "$it." } + classNames
private fun DRI.possiblyAsJava() = this.partialFqName().mapToJava()?.toDRI(this) ?: this
private fun TypeConstructor.possiblyAsJava() = when(this) {
    is GenericTypeConstructor -> copy(dri = this.dri.possiblyAsJava())
    is FunctionalTypeConstructor -> copy(dri = this.dri.possiblyAsJava())
}

private fun String.mapToJava(): ClassId? =
    JavaToKotlinClassMap.mapKotlinToJava(FqName(this).toUnsafe())

internal fun ClassId.toDRI(dri: DRI?): DRI = DRI(
    packageName = packageFqName.asString(),
    classNames = classNames(),
    callable = dri?.callable,//?.asJava(), TODO: check this
    extra = null,
    target = PointingToDeclaration
)

internal fun TypeConstructorWithKind.asJava(): TypeConstructorWithKind =
    TypeConstructorWithKind(
        typeConstructor = typeConstructor.possiblyAsJava(),
        kind = kind.asJava()
    )

internal fun ClassKind.asJava(): ClassKind {
    return when(this){
        is JavaClassKindTypes -> this
        KotlinClassKindTypes.CLASS -> JavaClassKindTypes.CLASS
        KotlinClassKindTypes.INTERFACE -> JavaClassKindTypes.INTERFACE
        KotlinClassKindTypes.ENUM_CLASS -> JavaClassKindTypes.ENUM_CLASS
        KotlinClassKindTypes.ENUM_ENTRY -> JavaClassKindTypes.ENUM_ENTRY
        KotlinClassKindTypes.ANNOTATION_CLASS -> JavaClassKindTypes.ANNOTATION_CLASS
        KotlinClassKindTypes.OBJECT -> JavaClassKindTypes.CLASS
        else -> throw IllegalStateException("Non exchaustive match while trying to convert $this to Java")
    }
}

private fun PropertyContainer<out Documentable>.mergeAdditionalModifiers(second: SourceSetDependent<Set<ExtraModifiers>>) =
    this[AdditionalModifiers]?.squash(AdditionalModifiers(second)) ?: AdditionalModifiers(second)

private fun AdditionalModifiers.squash(second: AdditionalModifiers) =
    AdditionalModifiers(content + second.content)

internal fun ClassId.classNames(): String =
    shortClassName.identifier + (outerClassId?.classNames()?.let { ".$it" } ?: "")
