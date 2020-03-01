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
            location.let { it.map.entries.first().value.path.split("/").last().split(".").first() + "Kt" } // TODO: first() does not look reasonable
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
                        nodes.filterIsInstance<Property>().map { it.javaAccessors() } +
                                nodes.filterIsInstance<Function>().map { it.asJava(syntheticClassName) }
                        ) as List<Function>, // TODO: methods are static and receiver is a param
                classlikes = emptyList(),
                sources = PlatformDependent.empty(),
                visibility = PlatformDependent.empty(), // TODO: fix this with the new visibility model -> public
                companion = null,
                generics = emptyList(),
                supertypes = PlatformDependent.empty(),
                documentation = PlatformDependent.empty(),
                modifier = WithAbstraction.Modifier.Final,
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
            WithAbstraction.Modifier.Final
        } else {
            WithAbstraction.Modifier.Empty
        },
        type = type.asJava(isTopLevel), // TODO: check,
        setter = null,
        getter = null // Removing getters and setters as they will be available as functions
    ) // TODO: visibility -> always private; if (isTopLevel) -> static

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
                WithAbstraction.Modifier.Final
            } else {
                WithAbstraction.Modifier.Empty
            },
            type = type.asJava(isTopLevel) // TODO: check,
        ),
        setter?.copy(
            dri = if (relocateToClass.isNullOrBlank()) {
                dri
            } else {
                dri.withClass(relocateToClass)
            },
            name = "set" + name.capitalize(),
            modifier = if (setter == null) {
                WithAbstraction.Modifier.Final
            } else {
                WithAbstraction.Modifier.Empty
            },
            type = type.asJava(isTopLevel) // TODO: check,
        )
    ) // TODO: if (isTopLevel) -> static; visibility -> always? public


internal fun Function.asJava(containingClassName: String): Function {
    val newName = when {
        isConstructor -> containingClassName
        else -> name
    }
    return copy(
//        dri = dri.copy(callable = dri.callable?.asJava()),
        name = newName,
        type = type.asJava(),
        parameters = parameters.map { it.asJava() }
    ) // TODO: should receiver be the first param?
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
        it.asJava(
            name
        )
    },
    properties = properties.map { it.asJava() },
    classlikes = classlikes.map { it.asJava() }
) // TODO: if modifier is from Kotlin, then Empty -> Final I think, Java ones stay the same

internal fun Enum.asJava(): Enum = copy(
    constructors = constructors.map { it.asJava(name) },
    functions = (functions + properties.map { it.getter } + properties.map { it.setter }).filterNotNull().map {
        it.asJava(
            name
        )
    },
    properties = properties.map { it.asJava() },
    classlikes = classlikes.map { it.asJava() }
//    , entries = entries.map { it.asJava() }
) // TODO: if modifier is from Kotlin, then Empty -> Final I think, Java ones stay the same

internal fun Object.asJava(): Object = copy(
    functions = (functions + properties.map { it.getter } + properties.map { it.setter })
        .filterNotNull()
        .map { it.asJava(name.orEmpty()) },
    properties = properties.map { it.asJava() } +
            Property(
                name = "INSTANCE",
                modifier = WithAbstraction.Modifier.Final,
                dri = dri.copy(callable = Callable("INSTANCE", null, emptyList())),
                documentation = PlatformDependent.empty(),
                sources = PlatformDependent.empty(),
                visibility = PlatformDependent.empty(), // TODO: public and static
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
                receiver = null
            ),
    classlikes = classlikes.map { it.asJava() }
)

internal fun Interface.asJava(): Interface = copy(
    functions = (functions + properties.map { it.getter } + properties.map { it.setter })
        .filterNotNull()
        .map { it.asJava(name) },
    properties = emptyList(),
    classlikes = classlikes.map { it.asJava() } // TODO: public static final class DefaultImpls with impls for methods (killme please)
)

internal fun Annotation.asJava(): Annotation = copy(
    properties = properties.map { it.asJava() },
    constructors = emptyList(),
    classlikes = classlikes.map { it.asJava() }
) // TODO investigate if annotation class can have methods and properties not from constructor

internal fun Parameter.asJava(): Parameter = copy(
    type = type.asJava()
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

internal fun TypeWrapper.asJava(top: Boolean = true): TypeWrapper = constructorFqName
    ?.takeUnless { it.endsWith(".Unit") } // TODO: ???
    ?.let { fqName -> fqName.mapToJava()?.let { getAsType(it, fqName, top) } } ?: this


private fun String.mapToJava(): ClassId? =
    JavaToKotlinClassMap.mapKotlinToJava(FqName(this).toUnsafe())

internal fun ClassId.toDRI(dri: DRI?): DRI = DRI(
    packageName = packageFqName.asString(),
    classNames = classNames(),
    callable = dri?.callable,//?.asJava(), TODO: ????
    extra = null,
    target = null
)

internal fun ClassId.classNames(): String =
    shortClassName.identifier + (outerClassId?.classNames()?.let { ".$it" } ?: "")

//fun TypeConstructor.asJava(): TypeReference =
//    fullyQualifiedName.mapToJava()
//        ?.let { tc.copy(fullyQualifiedName = it.asString(), params = tc.params.map { it.asJava() }) } ?: tc

//fun TypeParam.asJava(): TypeReference = copy(bounds = bounds.map { it.asJava() })

//fun TypeReference.asJava(): TypeReference = when (this) {
//    is TypeConstructor -> asJava()
//    is TypeParam -> asJava()
//    else -> this
//}