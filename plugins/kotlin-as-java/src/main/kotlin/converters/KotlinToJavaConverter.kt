package org.jetbrains.dokka.kotlinAsJava.converters

import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Annotation
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
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

internal fun Package.asJava(): Package {
    @Suppress("UNCHECKED_CAST")
    val syntheticClasses = ((properties + functions) as List<WithExpectActual>)
        .groupedByLocation()
        .map { (syntheticClassName, nodes) ->
            Class(
                dri = dri.withClass(syntheticClassName),
                name = syntheticClassName,
                properties = nodes.filterIsInstance<Property>().map { it.asJava() },
                constructors = emptyList(),
                functions = (
                        nodes.filterIsInstance<Property>()
                            .map { it.javaAccessors() } +
                                nodes.filterIsInstance<Function>()
                                    .map { it.asJava(syntheticClassName) }) as List<Function>, // TODO: methods are static and receiver is a param
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
                modifier = JavaModifier.Final,
                platformData = platformData,
                extra = PropertyContainer.empty()
            )
        }

    return copy(
        functions = emptyList(),
        properties = emptyList(),
        classlikes = classlikes.map { it.asJava() } + syntheticClasses,
        packages = packages.map { it.asJava() }
    )
}

internal fun Property.asJava(isTopLevel: Boolean = false, relocateToClass: String? = null) =
    copy(
        dri = if (relocateToClass.isNullOrBlank()) {
            dri
        } else {
            dri.withClass(relocateToClass)
        },
        modifier = if (setter == null) {
            JavaModifier.Final
        } else {
            JavaModifier.Empty
        },
        visibility = visibility.copy(
            map = visibility.mapValues { JavaVisibility.Private }
        ),
        type = type.asJava(isTopLevel), // TODO: check
        setter = null,
        getter = null, // Removing getters and setters as they will be available as functions
        extra = if (isTopLevel) extra.plus(extra.mergeAdditionalModifiers(listOf(ExtraModifiers.STATIC))) else extra
    )

internal fun Property.javaAccessors(isTopLevel: Boolean = false, relocateToClass: String? = null): List<Function> =
    listOfNotNull(
        getter?.copy(
            dri = if (relocateToClass.isNullOrBlank()) {
                dri
            } else {
                dri.withClass(relocateToClass)
            },
            name = "get" + name.capitalize(),
            modifier = if (setter == null) {
                JavaModifier.Final
            } else {
                JavaModifier.Empty
            },
            visibility = visibility.copy(
                map = visibility.mapValues { JavaVisibility.Public }
            ),
            type = type.asJava(isTopLevel), // TODO: check
            extra = if (isTopLevel) getter!!.extra.plus(getter!!.extra.mergeAdditionalModifiers(listOf(ExtraModifiers.STATIC))) else getter!!.extra
        ),
        setter?.copy(
            dri = if (relocateToClass.isNullOrBlank()) {
                dri
            } else {
                dri.withClass(relocateToClass)
            },
            name = "set" + name.capitalize(),
            modifier = if (setter == null) {
                JavaModifier.Final
            } else {
                JavaModifier.Empty
            },
            visibility = visibility.copy(
                map = visibility.mapValues { JavaVisibility.Public }
            ),
            type = type.asJava(isTopLevel), // TODO: check
            extra = if (isTopLevel) setter!!.extra.plus(setter!!.extra.mergeAdditionalModifiers(listOf(ExtraModifiers.STATIC))) else setter!!.extra
        )
    )


internal fun Function.asJava(containingClassName: String): Function {
    val newName = when {
        isConstructor -> containingClassName
        else -> name
    }
    return copy(
//        dri = dri.copy(callable = dri.callable?.asJava()),
        name = newName,
        type = type.asJava(),
        modifier = if(modifier is KotlinModifier.Final && isConstructor) JavaModifier.Empty else modifier,
        parameters = listOfNotNull(receiver?.asJava()) + parameters.map { it.asJava() },
        receiver = null
    ) // TODO static if toplevel
}

internal fun Classlike.asJava(): Classlike = when (this) {
    is Class -> asJava()
    is Enum -> asJava()
    is Annotation -> asJava()
    is Object -> asJava()
    is Interface -> asJava()
    else -> throw IllegalArgumentException("$this shouldn't be here")
}

internal fun Class.asJava(): Class = copy(
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
    modifier = if (modifier is KotlinModifier.Empty) JavaModifier.Final else modifier
)

private fun TypeParameter.asJava(): TypeParameter = copy(
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

internal fun Enum.asJava(): Enum = copy(
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

internal fun Object.asJava(): Object = copy(
    functions = (functions + properties.map { it.getter } + properties.map { it.setter })
        .filterNotNull()
        .map { it.asJava(name.orEmpty()) },
    properties = properties.map { it.asJava() } +
            Property(
                name = "INSTANCE",
                modifier = JavaModifier.Final,
                dri = dri.copy(callable = Callable("INSTANCE", null, emptyList())),
                documentation = PlatformDependent.empty(),
                sources = PlatformDependent.empty(),
                visibility = PlatformDependent(
                    platformData.map {
                        it to JavaVisibility.Public
                    }.toMap()
                ),
                type = JavaTypeWrapper(
                    dri.packageName?.split(".").orEmpty() +
                            dri.classNames?.split(".").orEmpty(),
                    emptyList(),
                    dri,
                    false
                ),
                setter = null,
                getter = null,
                platformData = platformData,
                receiver = null,
                extra = PropertyContainer.empty<Property>() + AdditionalModifiers(listOf(ExtraModifiers.STATIC))
            ),
    classlikes = classlikes.map { it.asJava() },
    supertypes = supertypes.copy(
        map = supertypes.mapValues { it.value.map { it.possiblyAsJava() } }
    )
)

internal fun Interface.asJava(): Interface = copy(
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

internal fun Annotation.asJava(): Annotation = copy(
    properties = properties.map { it.asJava() },
    constructors = emptyList(),
    classlikes = classlikes.map { it.asJava() }
) // TODO investigate if annotation class can have methods and properties not from constructor

internal fun Parameter.asJava(): Parameter = copy(
    type = type.asJava(),
    name = if (name.isNullOrBlank()) "\$self" else name
)

internal fun String.getAsPrimitive(): JvmPrimitiveType? = org.jetbrains.kotlin.builtins.PrimitiveType.values()
    .find { it.typeFqName.asString() == this }
    ?.let { JvmPrimitiveType.get(it) }

internal fun TypeWrapper.getAsType(classId: ClassId, fqName: String, top: Boolean): TypeWrapper {
    val fqNameSplit = fqName
        .takeIf { top }
        ?.getAsPrimitive()
        ?.name?.toLowerCase()
        ?.let(::listOf)
        ?: classId.asString().split("/")

    return JavaTypeWrapper(
        fqNameSplit,
        arguments.map { it.asJava(false) },
        classId.toDRI(dri),
        fqNameSplit.last()[0].isLowerCase()
    )
}

private fun DRI.partialFqName() = packageName?.let { "$it." } + classNames
private fun DRI.possiblyAsJava() = this.partialFqName().mapToJava()?.toDRI(this) ?: this

internal fun TypeWrapper.asJava(top: Boolean = true): TypeWrapper = constructorFqName
    ?.let { if (it.endsWith(".Unit")) return VoidTypeWrapper() else it }
    ?.let { fqName -> fqName.mapToJava()?.let { getAsType(it, fqName, top) } } ?: this

private data class VoidTypeWrapper(
    override val constructorFqName: String = "void",
    override val constructorNamePathSegments: List<String> = listOf("void"),
    override val arguments: List<TypeWrapper> = emptyList(),
    override val dri: DRI = DRI("java.lang", "Void")
) : TypeWrapper

private fun String.mapToJava(): ClassId? =
    JavaToKotlinClassMap.mapKotlinToJava(FqName(this).toUnsafe())

internal fun ClassId.toDRI(dri: DRI?): DRI = DRI(
    packageName = packageFqName.asString(),
    classNames = classNames(),
    callable = dri?.callable,//?.asJava(), TODO: check this
    extra = null,
    target = null
)

private fun PropertyContainer<out Documentable>.mergeAdditionalModifiers(second: List<ExtraModifiers>) =
    this[AdditionalModifiers.AdditionalKey]?.squash(AdditionalModifiers(second)) ?: AdditionalModifiers(second)

private fun AdditionalModifiers.squash(second: AdditionalModifiers) =
    AdditionalModifiers((content + second.content).distinct())

internal fun ClassId.classNames(): String =
    shortClassName.identifier + (outerClassId?.classNames()?.let { ".$it" } ?: "")