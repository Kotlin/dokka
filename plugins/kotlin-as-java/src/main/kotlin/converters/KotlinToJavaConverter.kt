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
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import java.lang.IllegalStateException

private fun <T : WithSources> List<T>.groupedByLocation() =
    map { it.sources to it }
        .groupBy({ (location, _) ->
            location.let {
                it.entries.first().value.path.split("/").last().split(".").first() + "Kt"
            } // TODO: first() does not look reasonable
        }) { it.second }

internal fun DPackage.asJava(): DPackage {
    @Suppress("UNCHECKED_CAST")
    val syntheticClasses = ((properties + functions) as List<WithSources>)
        .groupedByLocation()
        .map { (syntheticClassName, nodes) ->
            DClass(
                dri = dri.withClass(syntheticClassName),
                name = syntheticClassName,
                properties = nodes.filterIsInstance<DProperty>().map { it.asJava() },
                constructors = emptyList(),
                functions = (
                        nodes.filterIsInstance<DProperty>()
                            .flatMap { it.javaAccessors() } +
                                nodes.filterIsInstance<DFunction>()
                                    .map { it.asJava(syntheticClassName) }), // TODO: methods are static and receiver is a param
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

internal fun DProperty.asJava(isTopLevel: Boolean = false, relocateToClass: String? = null) =
    copy(
        dri = if (relocateToClass.isNullOrBlank()) {
            dri
        } else {
            dri.withClass(relocateToClass)
        },
        modifier = javaModifierFromSetter(),
        visibility = visibility.mapValues { it.value.propertyVisibilityAsJava() },
        type = type.asJava(), // TODO: check
        setter = null,
        getter = null, // Removing getters and setters as they will be available as functions
        extra = if (isTopLevel) extra +
                extra.mergeAdditionalModifiers(
                    sourceSets.map {
                        it to setOf(ExtraModifiers.JavaOnlyModifiers.Static)
                    }.toMap()
                )
        else extra
    )

internal fun DProperty.javaModifierFromSetter() =
    modifier.mapValues {
        when {
            it.value is JavaModifier -> it.value
            setter == null -> JavaModifier.Final
            else -> JavaModifier.Empty
        }
    }

internal fun DProperty.javaAccessors(isTopLevel: Boolean = false, relocateToClass: String? = null): List<DFunction> =
    listOfNotNull(
        getter?.copy(
            dri = if (relocateToClass.isNullOrBlank()) {
                dri
            } else {
                dri.withClass(relocateToClass)
            },
            name = "get" + name.capitalize(),
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
            dri = if (relocateToClass.isNullOrBlank()) {
                dri
            } else {
                dri.withClass(relocateToClass)
            },
            name = "set" + name.capitalize(),
            modifier = javaModifierFromSetter(),
            visibility = visibility.mapValues { JavaVisibility.Public },
            type = type.asJava(), // TODO: check
            extra = if (isTopLevel) setter!!.extra + setter!!.extra.mergeAdditionalModifiers(
                sourceSets.map {
                    it to setOf(ExtraModifiers.JavaOnlyModifiers.Static)
                }.toMap()
            )
            else setter!!.extra
        )
    )


internal fun DFunction.asJava(containingClassName: String): DFunction {
    val newName = when {
        isConstructor -> containingClassName
        else -> name
    }
    return copy(
//        dri = dri.copy(callable = dri.callable?.asJava()),
        name = newName,
        type = type.asJava(),
        modifier = if (modifier.all { (_, v) -> v is KotlinModifier.Final } && isConstructor)
            sourceSets.map { it to JavaModifier.Empty }.toMap()
        else sourceSets.map { it to modifier.values.first() }.toMap(),
        parameters = listOfNotNull(receiver?.asJava()) + parameters.map { it.asJava() },
        receiver = null
    ) // TODO static if toplevel
}

internal fun DClasslike.asJava(): DClasslike = when (this) {
    is DClass -> asJava()
    is DEnum -> asJava()
    is DAnnotation -> asJava()
    is DObject -> asJava()
    is DInterface -> asJava()
    else -> throw IllegalArgumentException("$this shouldn't be here")
}

internal fun DClass.asJava(): DClass = copy(
    constructors = constructors.map { it.asJava(name) },
    functions = (functions + properties.map { it.getter } + properties.map { it.setter }).filterNotNull().map {
        it.asJava(name)
    },
    properties = properties.map { it.asJava() },
    classlikes = classlikes.map { it.asJava() },
    generics = generics.map { it.asJava() },
    supertypes = supertypes.mapValues { it.value.map { it.asJava() } },
    modifier = if (modifier.all { (_, v) -> v is KotlinModifier.Empty }) sourceSets.map { it to JavaModifier.Final }
        .toMap()
    else sourceSets.map { it to modifier.values.first() }.toMap()
)

private fun DTypeParameter.asJava(): DTypeParameter = copy(
    variantTypeParameter = variantTypeParameter.withDri(dri.possiblyAsJava()),
    bounds = bounds.map { it.asJava() as Bound }
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
    is TypeConstructor -> copy(
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
    constructors = constructors.map { it.asJava(name) },
    functions = (functions + properties.map { it.getter } + properties.map { it.setter }).filterNotNull().map {
        it.asJava(name)
    },
    properties = properties.map { it.asJava() },
    classlikes = classlikes.map { it.asJava() },
    supertypes = supertypes.mapValues { it.value.map { it.asJava() } }
//    , entries = entries.map { it.asJava() }
)

internal fun DObject.asJava(): DObject = copy(
    functions = (functions + properties.map { it.getter } + properties.map { it.setter })
        .filterNotNull()
        .map { it.asJava(name.orEmpty()) },
    properties = properties.map { it.asJava() } +
            DProperty(
                name = "INSTANCE",
                modifier = sourceSets.map { it to JavaModifier.Final }.toMap(),
                dri = dri.copy(callable = Callable("INSTANCE", null, emptyList())),
                documentation = emptyMap(),
                sources = emptyMap(),
                visibility = sourceSets.map {
                    it to JavaVisibility.Public
                }.toMap(),
                type = TypeConstructor(dri, emptyList()),
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
    classlikes = classlikes.map { it.asJava() },
    supertypes = supertypes.mapValues { it.value.map { it.asJava() } }
)

internal fun DInterface.asJava(): DInterface = copy(
    functions = (functions + properties.map { it.getter } + properties.map { it.setter })
        .filterNotNull()
        .map { it.asJava(name) },
    properties = emptyList(),
    classlikes = classlikes.map { it.asJava() }, // TODO: public static final class DefaultImpls with impls for methods
    generics = generics.map { it.asJava() },
    supertypes = supertypes.mapValues { it.value.map { it.asJava() } }
)

internal fun DAnnotation.asJava(): DAnnotation = copy(
    properties = properties.map { it.asJava() },
    constructors = emptyList(),
    classlikes = classlikes.map { it.asJava() }
) // TODO investigate if annotation class can have methods and properties not from constructor

internal fun DParameter.asJava(): DParameter = copy(
    type = type.asJava(),
    name = if (name.isNullOrBlank()) "\$self" else name
)

internal fun Visibility.propertyVisibilityAsJava(): Visibility =
    if(this is JavaVisibility) this
    else JavaVisibility.Private

internal fun String.getAsPrimitive(): JvmPrimitiveType? = org.jetbrains.kotlin.builtins.PrimitiveType.values()
    .find { it.typeFqName.asString() == this }
    ?.let { JvmPrimitiveType.get(it) }

private fun DRI.partialFqName() = packageName?.let { "$it." } + classNames
private fun DRI.possiblyAsJava() = this.partialFqName().mapToJava()?.toDRI(this) ?: this
private fun TypeConstructor.possiblyAsJava() = copy(dri = this.dri.possiblyAsJava())

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
