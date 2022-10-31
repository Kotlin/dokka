package org.jetbrains.dokka.kotlinAsJava.converters

import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer

private const val DEFAULT_COMPANION_NAME = "Companion"

internal fun DObject?.staticFunctionsForJava(): List<DFunction> {
    if (this == null) return emptyList()
    return functions.filter { it.isJvmStatic }
}

/**
 * @return properties that will be visible as static for java.
 * See [Static fields](https://kotlinlang.org/docs/java-to-kotlin-interop.html#static-fields)
 */
internal fun DObject?.staticPropertiesForJava(): List<DProperty> {
    if (this == null) return emptyList()
    return properties.filter { it.isJvmField || it.isConst || it.isLateInit }
}

internal fun DObject.companionInstancePropertyForJava(): DProperty? {
    if (hasNothingToRender()) return null // do not show if companion not rendered

    return DProperty(
        name = name ?: DEFAULT_COMPANION_NAME,
        modifier = sourceSets.associateWith { JavaModifier.Final },
        dri = dri.copy(callable = Callable(name ?: DEFAULT_COMPANION_NAME, null, emptyList())),
        documentation = emptyMap(),
        sources = emptyMap(),
        visibility = sourceSets.associateWith {
            JavaVisibility.Public
        },
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
    )
}

internal fun DObject.companionAsJava(): DObject? {
    if (hasNothingToRender()) return null

    return asJava(
        excludedProps = staticPropertiesForJava(),
        excludedFunctions = staticFunctionsForJava()
    )
}

/**
 * Hide companion object if there isn't members of parents.
 * Properties and functions that are moved to outer class are not counted as members.
 */
private fun DObject.hasNothingToRender(): Boolean {
    val nonStaticPropsCount = properties.size - staticPropertiesForJava().size
    val nonStaticFunctionsCount = functions.size - staticFunctionsForJava().size
    val classLikesCount = classlikes.size
    val superTypesCount = supertypes.values.firstOrNull()?.size ?: 0

    return nonStaticFunctionsCount + nonStaticPropsCount +
            classLikesCount + superTypesCount == 0
}
