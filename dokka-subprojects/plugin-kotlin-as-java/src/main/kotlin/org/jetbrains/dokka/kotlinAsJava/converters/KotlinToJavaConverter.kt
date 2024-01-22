/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinAsJava.converters

import org.jetbrains.dokka.kotlinAsJava.*
import org.jetbrains.dokka.kotlinAsJava.transformers.JvmNameProvider
import org.jetbrains.dokka.kotlinAsJava.transformers.withCallableName
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin

public val jvmNameProvider: JvmNameProvider = JvmNameProvider()
internal const val OBJECT_INSTANCE_NAME = "INSTANCE"

internal val DProperty.isConst: Boolean
    get() = hasModifier(ExtraModifiers.KotlinOnlyModifiers.Const)

internal val DProperty.isLateInit: Boolean
    get() = hasModifier(ExtraModifiers.KotlinOnlyModifiers.LateInit)

internal val DProperty.isJvmField: Boolean
    get() = jvmField() != null

internal val DFunction.isJvmStatic: Boolean
    get() = jvmStatic() != null

private fun DProperty.hasModifier(modifier: ExtraModifiers.KotlinOnlyModifiers): Boolean =
    extra[AdditionalModifiers]
        ?.content
        ?.any { (_, modifiers) -> modifier in modifiers } == true

public class KotlinToJavaConverter(
    private val context: DokkaContext
) {
    private val kotlinToJavaMapper by lazy {
        context.plugin<InternalKotlinAnalysisPlugin>().querySingle { kotlinToJavaService }
    }

    internal fun DPackage.asJava(): DPackage {
        val syntheticClasses =
            (properties.map { jvmNameProvider.nameForSyntheticClass(it) to it }
                    + functions.map { jvmNameProvider.nameForSyntheticClass(it) to it })
                .groupBy({ it.first }) { it.second }
                .map { (syntheticClassName, nodes) ->
                    DClass(
                        dri = dri.withClass(syntheticClassName.name),
                        name = syntheticClassName.name,
                        properties = nodes
                            .filterIsInstance<DProperty>()
                            .filterNot { it.hasJvmSynthetic() }
                            .map { it.asJava(true) },
                        constructors = emptyList(),
                        functions = (
                                nodes
                                    .filterIsInstance<DProperty>()
                                    .filterNot { it.isConst || it.isJvmField || it.hasJvmSynthetic() }
                                    .flatMap { it.javaAccessors(relocateToClass = syntheticClassName.name) } +
                                        nodes
                                            .filterIsInstance<DFunction>()
                                            .flatMap { it.asJava(syntheticClassName.name, true) })
                            .filterNot { it.hasJvmSynthetic() },
                        classlikes = emptyList(),
                        sources = emptyMap(),
                        expectPresentInSet = null,
                        visibility = sourceSets.associateWith {
                            JavaVisibility.Public
                        },
                        companion = null,
                        generics = emptyList(),
                        supertypes = emptyMap(),
                        documentation = emptyMap(),
                        modifier = sourceSets.associateWith { JavaModifier.Final },
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

    internal fun DProperty.asJava(
        isTopLevel: Boolean = false,
        relocateToClass: String? = null,
        isFromObjectOrCompanion: Boolean = false
    ) =
        copy(
            dri = if (relocateToClass.isNullOrBlank()) {
                dri
            } else {
                dri.withClass(relocateToClass)
            },
            modifier = javaModifierFromSetter(),
            visibility = visibility.mapValues {
                if (isConst || isJvmField || (getter == null && setter == null) || (isFromObjectOrCompanion && isLateInit)) {
                    it.value.asJava()
                } else {
                    it.value.propertyVisibilityAsJava()
                }
            },
            type = type.asJava(), // TODO: check
            setter = null,
            getter = null, // Removing getters and setters as they will be available as functions
            extra = if (isTopLevel || isConst || (isFromObjectOrCompanion && isJvmField) || (isFromObjectOrCompanion && isLateInit))
                extra + extra.mergeAdditionalModifiers(
                    sourceSets.associateWith {
                        setOf(ExtraModifiers.JavaOnlyModifiers.Static)
                    }
                )
            else extra
        )

    internal fun Visibility.asJava() =
        when (this) {
            is JavaVisibility -> this
            is KotlinVisibility.Public, KotlinVisibility.Internal -> JavaVisibility.Public
            is KotlinVisibility.Private -> JavaVisibility.Private
            is KotlinVisibility.Protected -> JavaVisibility.Protected
        }

    internal fun DProperty.javaModifierFromSetter() =
        modifier.mapValues {
            when {
                it.value is JavaModifier -> it.value
                setter == null -> JavaModifier.Final
                else -> JavaModifier.Empty
            }
        }

    internal fun DProperty.javaAccessors(
        isTopLevel: Boolean = false,
        relocateToClass: String? = null
    ): List<DFunction> =
        listOfNotNull(
            getter?.let { getter ->
                val name = "get" + name.capitalize()
                getter.copy(
                    dri = if (relocateToClass.isNullOrBlank()) {
                        getter.dri
                    } else {
                        getter.dri.withClass(relocateToClass)
                    }.withCallableName(name),
                    name = name,
                    modifier = javaModifierFromSetter(),
                    visibility = visibility.mapValues { JavaVisibility.Public },
                    type = getter.type.asJava(),
                    extra = if (isTopLevel) getter.extra +
                            getter.extra.mergeAdditionalModifiers(
                                sourceSets.associateWith {
                                    setOf(ExtraModifiers.JavaOnlyModifiers.Static)
                                }
                            )
                    else getter.extra
                )
            },
            setter?.let { setter ->
                val name = "set" + name.capitalize()
                val baseDRI = (if (relocateToClass.isNullOrBlank()) {
                    setter.dri
                } else {
                    setter.dri.withClass(relocateToClass)
                }).withCallableName(name)
                setter.copy(
                    dri = baseDRI,
                    name = name,
                    parameters = setter.parameters.map {
                        it.copy(
                            dri = baseDRI.copy(
                                target = it.dri.target,
                                extra = it.dri.extra
                            ), type = it.type.asJava()
                        )
                    },
                    modifier = javaModifierFromSetter(),
                    visibility = visibility.mapValues { JavaVisibility.Public },
                    type = Void,
                    extra = if (isTopLevel) setter.extra + setter.extra.mergeAdditionalModifiers(
                        sourceSets.associateWith {
                            setOf(ExtraModifiers.JavaOnlyModifiers.Static)
                        }
                    )
                    else setter.extra
                )
            }
        )

    private fun DFunction.asJava(
        containingClassName: String,
        newName: String,
        parameters: List<DParameter>,
        isTopLevel: Boolean = false
    ): DFunction {
        return copy(
            dri = dri.copy(classNames = containingClassName, callable = dri.callable?.copy(name = newName)),
            name = newName,
            type = type.asJava(),
            modifier = if (modifier.all { (_, v) -> v is KotlinModifier.Final } && isConstructor)
                sourceSets.associateWith { JavaModifier.Empty }
            else sourceSets.associateWith { modifier.values.first() },
            parameters = listOfNotNull(receiver?.asJava()) + parameters.map { it.asJava() },
            visibility = visibility.map { (sourceSet, visibility) -> Pair(sourceSet, visibility.asJava()) }.toMap(),
            receiver = null,
            extra = if (isTopLevel || isJvmStatic) {
                extra + extra.mergeAdditionalModifiers(
                    sourceSets.associateWith {
                        setOf(ExtraModifiers.JavaOnlyModifiers.Static)
                    }
                )
            } else {
                extra
            }
        )
    }

    private fun DFunction.withJvmOverloads(
        containingClassName: String,
        newName: String,
        isTopLevel: Boolean = false
    ): List<DFunction>? {
        val (paramsWithDefaults, paramsWithoutDefaults) = parameters
            .withIndex()
            .partition { (_, p) -> p.extra[DefaultValue] != null }
        return paramsWithDefaults
            .runningFold(paramsWithoutDefaults) { acc, param -> (acc + param) }
            .map { params ->
                asJava(
                    containingClassName,
                    newName,
                    params
                        .sortedBy(IndexedValue<DParameter>::index)
                        .map { it.value },
                    isTopLevel
                )
            }
            .reversed()
            .takeIf { it.isNotEmpty() }
    }

    internal fun DFunction.asJava(containingClassName: String, isTopLevel: Boolean = false): List<DFunction> {
        val newName = when {
            isConstructor -> containingClassName
            else -> name
        }
        val baseFunction = asJava(containingClassName, newName, parameters, isTopLevel)
        return if (hasJvmOverloads()) {
            withJvmOverloads(containingClassName, newName, isTopLevel) ?: listOf(baseFunction)
        } else {
            listOf(baseFunction)
        }
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
        constructors = constructors
            .filterNot { it.hasJvmSynthetic() }
            .flatMap {
                it.asJava(
                    dri.classNames ?: name
                )
            }, // name may not always be valid here, however classNames should always be not null
        functions = functionsInJava(),
        properties = propertiesInJava(),
        classlikes = classlikesInJava(),
        generics = generics.map { it.asJava() },
        companion = companion?.companionAsJava(),
        supertypes = supertypes.mapValues { it.value.map { it.asJava() } },
        modifier = if (modifier.all { (_, v) -> v is KotlinModifier.Empty }) sourceSets.associateWith { JavaModifier.Final }
        else sourceSets.associateWith { modifier.values.first() }
    )

    /**
     * Companion objects requires some custom logic for rendering as Java.
     * They are excluded from usual classlikes rendering and added after.
     */
    internal fun DClass.classlikesInJava(): List<DClasslike> {
        val classlikes = classlikes
            .filter { it.name != companion?.name }
            .map { it.asJava() }

        val companionAsJava = companion?.companionAsJava()
        return if (companionAsJava != null) classlikes.plus(companionAsJava) else classlikes
    }


    internal fun DClass.functionsInJava(): List<DFunction> =
        properties
            .filter { !it.isJvmField && !it.hasJvmSynthetic() }
            .flatMap { property -> listOfNotNull(property.getter, property.setter) }
            .plus(functions)
            .plus(companion.staticFunctionsForJava())
            .filterNot { it.hasJvmSynthetic() }
            .flatMap { it.asJava(it.dri.classNames ?: it.name) }

    internal fun DClass.propertiesInJava(): List<DProperty> {
        val propertiesFromCompanion = companion
            .staticPropertiesForJava()
            .filterNot { it.hasJvmSynthetic() }
            .map { it.asJava(isFromObjectOrCompanion = true) }
        val companionInstanceProperty = companion?.companionInstancePropertyForJava()
        val ownProperties = properties
            .filterNot { it.hasJvmSynthetic() }
            .map { it.asJava() }

        return propertiesFromCompanion + ownProperties + listOfNotNull(companionInstanceProperty)
    }

    private fun DTypeParameter.asJava(): DTypeParameter = copy(
        variantTypeParameter = variantTypeParameter.withDri(dri.possiblyAsJava()),
        bounds = bounds.map { it.asJava() }
    )

    private fun Projection.asJava(): Projection = when (this) {
        is Star -> Star
        is Covariance<*> -> copy(inner.asJava())
        is Contravariance<*> -> copy(inner.asJava())
        is Invariance<*> -> copy(inner.asJava())
        is Bound -> asJava()
    }

    private fun Bound.asJava(): Bound = when (this) {
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
        is DefinitelyNonNullable -> copy(inner.asJava())
        is PrimitiveJavaType -> this
        is Void -> this
        is JavaObject -> this
        is Dynamic -> this
        is UnresolvedBound -> this
    }

    internal fun DEnum.asJava(): DEnum = copy(
        constructors = constructors.flatMap { it.asJava(dri.classNames ?: name) },
        functions = functions
            .plus(
                properties
                    .filter { !it.isJvmField && !it.hasJvmSynthetic() }
                    .flatMap { listOf(it.getter, it.setter) }
            )
            .filterNotNull()
            .filterNot { it.hasJvmSynthetic() }
            .flatMap { it.asJava(dri.classNames ?: name) },
        properties = properties
            .filterNot { it.hasJvmSynthetic() }
            .map { it.asJava() },
        classlikes = classlikes.map { it.asJava() },
        supertypes = supertypes.mapValues { it.value.map { it.asJava() } }
//    , entries = entries.map { it.asJava() }
    )

    /**
     * Parameters [excludedProps] and [excludedFunctions] used for rendering companion objects
     * where some members (that lifted to outer class) are not rendered
     */
    internal fun DObject.asJava(
        excludedProps: List<DProperty> = emptyList(),
        excludedFunctions: List<DFunction> = emptyList()
    ): DObject = copy(
        functions = functions
            .plus(
                properties
                    .filterNot { it in excludedProps }
                    .filter { !it.isJvmField && !it.isConst && !it.isLateInit && !it.hasJvmSynthetic() }
                    .flatMap { listOf(it.getter, it.setter) }
            )
            .filterNotNull()
            .filterNot { it in excludedFunctions }
            .filterNot { it.hasJvmSynthetic() }
            .flatMap { it.asJava(dri.classNames ?: name.orEmpty()) },
        properties = properties
            .filterNot { it.hasJvmSynthetic() }
            .filterNot { it in excludedProps }
            .map { it.asJava(isFromObjectOrCompanion = true) } +
                DProperty(
                    name = OBJECT_INSTANCE_NAME,
                    modifier = sourceSets.associateWith { JavaModifier.Final },
                    dri = dri.copy(callable = Callable(OBJECT_INSTANCE_NAME, null, emptyList())),
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
                ),
        classlikes = classlikes.map { it.asJava() },
        supertypes = supertypes.mapValues { it.value.map { it.asJava() } }
    )

    internal fun DInterface.asJava(): DInterface = copy(
        functions = functions
            .plus(
                properties
                    .filter { it.jvmField() == null && !it.hasJvmSynthetic() }
                    .flatMap { listOf(it.getter, it.setter) }
            )
            .filterNotNull()
            .filterNot { it.hasJvmSynthetic() }
            .flatMap { it.asJava(dri.classNames ?: name) },
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
        if (this is JavaVisibility) this
        else JavaVisibility.Private

    private fun TypeConstructor.possiblyAsJava(): TypeConstructor = when (this) {
        is GenericTypeConstructor -> copy(dri = this.dri.possiblyAsJava())
        is FunctionalTypeConstructor -> copy(dri = this.dri.possiblyAsJava())
    }


    internal fun TypeConstructorWithKind.asJava(): TypeConstructorWithKind =
        TypeConstructorWithKind(
            typeConstructor = typeConstructor.possiblyAsJava(),
            kind = kind.asJava()
        )

    internal fun ClassKind.asJava(): ClassKind {
        return when (this) {
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

    private fun <T : Documentable> PropertyContainer<T>.mergeAdditionalModifiers(second: SourceSetDependent<Set<ExtraModifiers>>) =
        this[AdditionalModifiers]?.squash(AdditionalModifiers(second)) ?: AdditionalModifiers(second)

    private fun AdditionalModifiers.squash(second: AdditionalModifiers) =
        AdditionalModifiers(content + second.content)

    internal fun DObject.companionAsJava(): DObject? {
        if (hasNothingToRender()) return null

        return asJava(
            excludedProps = staticPropertiesForJava(),
            excludedFunctions = staticFunctionsForJava()
        )
    }

    private fun DRI.possiblyAsJava(): DRI {
        return kotlinToJavaMapper.findAsJava(this) ?: this
    }
}
