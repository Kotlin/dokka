package org.jetbrains.dokka.kotlinAsJava.converters

import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

private fun <T : WithExpectActual> List<T>.groupedByLocation() =
    map { it.sources to it }
        .groupBy({ (location, _) ->
            location.let {
                it.map.entries.first().value.path.split("/").last().split(".").first() + "Kt"
            } // TODO: first() does not look reasonable
        }) { it.second }

internal fun DPackage.asJava(): DPackage {
    @Suppress("UNCHECKED_CAST")
    val syntheticClasses = ((properties + functions) as List<WithExpectActual>)
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
                sources = PlatformDependent.empty(),
                visibility = PlatformDependent(
                    platformData.map {
                        it to JavaVisibility.Public
                    }.toMap()
                ),
                companion = null,
                generics = emptyList(),
                supertypes = PlatformDependent.empty(),
                documentation = PlatformDependent.empty(),
                modifier = PlatformDependent(map = platformData.map{ it to JavaModifier.Final}.toMap()),
                platformData = platformData,
                extra = PropertyContainer.empty()
            )
        }

    return copy(
        functions = emptyList(),
        properties = emptyList(),
        classlikes = classlikes.map { it.asJava() } + syntheticClasses
    )
}

internal fun DProperty.asJava(isTopLevel: Boolean = false, relocateToClass: String? = null) =
    copy(
        dri = if (relocateToClass.isNullOrBlank()) {
            dri
        } else {
            dri.withClass(relocateToClass)
        },
        modifier = if (setter == null) {
            PlatformDependent(map = platformData.map{it to JavaModifier.Final}.toMap())
        } else {
            PlatformDependent(map = platformData.map{it to JavaModifier.Empty}.toMap())
        },
        visibility = visibility.copy(
            map = visibility.mapValues { JavaVisibility.Private }
        ),
        type = type.asJava(), // TODO: check
        setter = null,
        getter = null, // Removing getters and setters as they will be available as functions
        extra = if (isTopLevel) extra.plus(extra.mergeAdditionalModifiers(setOf(ExtraModifiers.STATIC))) else extra
    )

internal fun DProperty.javaAccessors(isTopLevel: Boolean = false, relocateToClass: String? = null): List<DFunction> =
    listOfNotNull(
        getter?.copy(
            dri = if (relocateToClass.isNullOrBlank()) {
                dri
            } else {
                dri.withClass(relocateToClass)
            },
            name = "get" + name.capitalize(),
            modifier = if (setter == null) {
                PlatformDependent(map = platformData.map{it to JavaModifier.Final}.toMap())
            } else {
                PlatformDependent(map = platformData.map{it to JavaModifier.Empty}.toMap())
            },
            visibility = visibility.copy(
                map = visibility.mapValues { JavaVisibility.Public }
            ),
            type = type.asJava(), // TODO: check
            extra = if (isTopLevel) getter!!.extra.plus(getter!!.extra.mergeAdditionalModifiers(setOf(ExtraModifiers.STATIC))) else getter!!.extra
        ),
        setter?.copy(
            dri = if (relocateToClass.isNullOrBlank()) {
                dri
            } else {
                dri.withClass(relocateToClass)
            },
            name = "set" + name.capitalize(),
            modifier = if (setter == null) {
                PlatformDependent(map = platformData.map{it to JavaModifier.Final}.toMap())
            } else {
                PlatformDependent(map = platformData.map{it to JavaModifier.Empty}.toMap())
            },
            visibility = visibility.copy(
                map = visibility.mapValues { JavaVisibility.Public }
            ),
            type = type.asJava(), // TODO: check
            extra = if (isTopLevel) setter!!.extra.plus(setter!!.extra.mergeAdditionalModifiers(setOf(ExtraModifiers.STATIC))) else setter!!.extra
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
        modifier = if(modifier.all{(_,v)-> v is KotlinModifier.Final} && isConstructor)
            PlatformDependent(map = platformData.map{it to JavaModifier.Empty}.toMap())
        else PlatformDependent(map = platformData.map{it to modifier.allValues.first()}.toMap()),
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
    supertypes = supertypes.copy(
        map = supertypes.mapValues { it.value.map { it.possiblyAsJava() } }
    ),
    modifier = if (modifier.all{(_,v) -> v is KotlinModifier.Empty}) PlatformDependent(map = platformData.map{it to JavaModifier.Final}.toMap())
    else PlatformDependent(map = platformData.map{it to modifier.allValues.first()}.toMap())
)

private fun DTypeParameter.asJava(): DTypeParameter = copy(
    dri = dri.possiblyAsJava(),
    bounds = bounds.map { it.asJava() }
)

private fun Bound.asJava(): Bound = when (this) {
    is TypeConstructor -> copy(
        dri = dri.possiblyAsJava()
    )
    is Nullable -> copy(
        inner = inner.asJava()
    )
    else -> this
}

internal fun DEnum.asJava(): DEnum = copy(
    constructors = constructors.map { it.asJava(name) },
    functions = (functions + properties.map { it.getter } + properties.map { it.setter }).filterNotNull().map {
        it.asJava(name)
    },
    properties = properties.map { it.asJava() },
    classlikes = classlikes.map { it.asJava() },
    supertypes = supertypes.copy(
        map = supertypes.mapValues { it.value.map { it.possiblyAsJava() } }
    )
//    , entries = entries.map { it.asJava() }
)

internal fun DObject.asJava(): DObject = copy(
    functions = (functions + properties.map { it.getter } + properties.map { it.setter })
        .filterNotNull()
        .map { it.asJava(name.orEmpty()) },
    properties = properties.map { it.asJava() } +
            DProperty(
                name = "INSTANCE",
                modifier = PlatformDependent(map = platformData.map{it to JavaModifier.Final}.toMap()),
                dri = dri.copy(callable = Callable("INSTANCE", null, emptyList())),
                documentation = PlatformDependent.empty(),
                sources = PlatformDependent.empty(),
                visibility = PlatformDependent(
                    platformData.map {
                        it to JavaVisibility.Public
                    }.toMap()
                ),
                type = TypeConstructor(dri, emptyList()),
                setter = null,
                getter = null,
                platformData = platformData,
                receiver = null,
                extra = PropertyContainer.empty<DProperty>() + AdditionalModifiers(setOf(ExtraModifiers.STATIC))
            ),
    classlikes = classlikes.map { it.asJava() },
    supertypes = supertypes.copy(
        map = supertypes.mapValues { it.value.map { it.possiblyAsJava() } }
    )
)

internal fun DInterface.asJava(): DInterface = copy(
    functions = (functions + properties.map { it.getter } + properties.map { it.setter })
        .filterNotNull()
        .map { it.asJava(name) },
    properties = emptyList(),
    classlikes = classlikes.map { it.asJava() }, // TODO: public static final class DefaultImpls with impls for methods
    generics = generics.map { it.asJava() },
    supertypes = supertypes.copy(
        map = supertypes.mapValues { it.value.map { it.possiblyAsJava() } }
    )
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

internal fun String.getAsPrimitive(): JvmPrimitiveType? = org.jetbrains.kotlin.builtins.PrimitiveType.values()
    .find { it.typeFqName.asString() == this }
    ?.let { JvmPrimitiveType.get(it) }

private fun DRI.partialFqName() = packageName?.let { "$it." } + classNames
private fun DRI.possiblyAsJava() = this.partialFqName().mapToJava()?.toDRI(this) ?: this

private fun String.mapToJava(): ClassId? =
    JavaToKotlinClassMap.mapKotlinToJava(FqName(this).toUnsafe())

internal fun ClassId.toDRI(dri: DRI?): DRI = DRI(
    packageName = packageFqName.asString(),
    classNames = classNames(),
    callable = dri?.callable,//?.asJava(), TODO: check this
    extra = null,
    target = null
)

private fun PropertyContainer<out Documentable>.mergeAdditionalModifiers(second: Set<ExtraModifiers>) =
    this[AdditionalModifiers]?.squash(AdditionalModifiers(second)) ?: AdditionalModifiers(second)

private fun AdditionalModifiers.squash(second: AdditionalModifiers) =
    AdditionalModifiers(content + second.content)

internal fun ClassId.classNames(): String =
    shortClassName.identifier + (outerClassId?.classNames()?.let { ".$it" } ?: "")