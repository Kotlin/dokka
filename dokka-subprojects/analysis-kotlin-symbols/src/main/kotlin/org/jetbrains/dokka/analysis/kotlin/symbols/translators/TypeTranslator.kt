/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.AnnotationTranslator.Companion.getPresentableName
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap

internal const val ERROR_CLASS_NAME = "<ERROR CLASS>"

/**
 * Maps [KaType] to Dokka [Bound] or [TypeConstructorWithKind].
 *
 * Also, build [AncestryNode] tree from [KaType]
 */
internal class TypeTranslator(
    private val sourceSet: DokkaConfiguration.DokkaSourceSet,
    private val annotationTranslator: AnnotationTranslator,
) {

    private fun <T> T.toSourceSetDependent() = if (this != null) mapOf(sourceSet to this) else emptyMap()

    /**
     * @param isJavaContext when true, invariant type arguments are returned as raw bounds
     *   (matching PSI translator behavior for Java types) instead of being wrapped in [Invariance].
     */
    private fun KaSession.toProjection(typeProjection: KaTypeProjection, isJavaContext: Boolean = false): Projection =
        when (typeProjection) {
            is KaStarTypeProjection -> if (isJavaContext) Covariance(JavaObject()) else Star
            is KaTypeArgumentWithVariance -> {
                val bound = toBoundFrom(typeProjection.type, isJavaContext)
                if (isJavaContext && typeProjection.variance == org.jetbrains.kotlin.types.Variance.INVARIANT) {
                    bound
                } else {
                    // In Java context, strip Nullable wrapper from wildcard bounds
                    // (Java wildcards like ? extends Object don't have nullability)
                    val effectiveBound = if (isJavaContext && bound is Nullable) bound.inner else bound
                    effectiveBound.wrapWithVariance(typeProjection.variance)
                }
            }
        }

    /**
     * For example,
     * ```
     * typealias Inner = String
     * typealias Outer = Inner
     *
     * val outer: Outer = ""
     * ```
     *
     * `Outer` is [abbreviationType]
     * `String` is [fullyExpandedType]
     */
    private fun KaSession.toBoundFromTypeAliased(abbreviationType: KaClassType, fullyExpandedType: Bound): TypeAliased {
        val classSymbol = abbreviationType.symbol
        return if (classSymbol is KaTypeAliasSymbol)
            TypeAliased(
                typeAlias = asNullableIfMarked(
                    abbreviationType,
                    bound = GenericTypeConstructor(
                        dri = getDRIFromClassType(abbreviationType),
                        projections = abbreviationType.typeArguments.map { toProjection(it) }
                    )
                ),
                inner = fullyExpandedType,
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(abbreviationType)?.toSourceSetDependent()?.toAnnotations()
                )
            ) else
            throw IllegalStateException("Expected type alias symbol in type")
    }

    private fun KaSession.toTypeConstructorFrom(classType: KaClassType, isJavaContext: Boolean = false): GenericTypeConstructor {
        var dri = getDRIFromClassType(classType)
        // In Java context, reverse-map Kotlin-mapped types back to their Java originals
        // (e.g., kotlin.String → java.lang.String, kotlin.collections.List → java.util.List)
        if (isJavaContext) {
            val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(classType.classId.asSingleFqName().toUnsafe())
            if (javaClassId != null) {
                dri = javaClassId.createDRI()
            }
        }
        return GenericTypeConstructor(
            dri = dri,
            projections = classType.typeArguments.map { toProjection(it, isJavaContext || classType.symbol.isJavaSource()) },
            presentableName = classType.getPresentableName(),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(classType)?.toSourceSetDependent()?.toAnnotations()
            )
        )
    }

    private fun KaSession.toFunctionalTypeConstructorFrom(functionalType: KaFunctionType) = FunctionalTypeConstructor(
        dri = getDRIFromClassType(functionalType),
        projections = functionalType.typeArguments.map { toProjection(it) },
        isExtensionFunction = functionalType.receiverType != null,
        isSuspendable = functionalType.isSuspend,
        presentableName = functionalType.getPresentableName(),
        extra = PropertyContainer.withAll(
            getDokkaAnnotationsFrom(functionalType)?.toSourceSetDependent()?.toAnnotations()
        ),
        contextParametersCount = @OptIn(KaExperimentalApi::class) functionalType.contextReceivers.size
    )

    // TODO: the main problem is really with mapped types
    //  e.g. java.lang.Object and kotlin.Any
    //  or primitives, or void, etc...
    //  there are also small other changes, but they are minor

    fun KaSession.toBoundFrom(type: KaType, isJavaContext: Boolean = false): Bound {
        val abbreviation = type.abbreviation
        val bound = toBoundFromNoAbbreviation(type, isJavaContext)
        return when {
            abbreviation != null -> toBoundFromTypeAliased(abbreviation, bound)
            else -> bound
        }
    }

    fun KaSession.toBoundFromNoAbbreviation(type: KaType, isJavaContext: Boolean = false): Bound =
        when (type) {
            is KaUsualClassType -> {
                val classId = type.classId
                val fqName = classId.asFqNameString()
                when {
                    // Map java.lang.Object / kotlin.Any to JavaObject in Java context
                    fqName == "java.lang.Object" || (isJavaContext && fqName == "kotlin.Any") -> JavaObject()
                    // Map kotlin.Unit to Void in Java context (Java void methods)
                    isJavaContext && fqName == "kotlin.Unit" -> Void
                    // Map Kotlin primitive types to PrimitiveJavaType in Java context
                    isJavaContext && kotlinPrimitiveToJava.containsKey(fqName) -> PrimitiveJavaType(kotlinPrimitiveToJava[fqName]!!)
                    // Map Kotlin primitive array types to Array<PrimitiveJavaType> in Java context
                    isJavaContext && kotlinPrimitiveArrayToJava.containsKey(fqName) -> GenericTypeConstructor(
                        dri = DRI("kotlin", "Array"),
                        projections = listOf(PrimitiveJavaType(kotlinPrimitiveArrayToJava[fqName]!!))
                    )
                    else -> toTypeConstructorFrom(type, isJavaContext)
                }
            }
            is KaTypeParameterType -> TypeParameter(
                dri = getDRIFromTypeParameter(type.symbol),
                name = type.name.asString(),
                presentableName = type.getPresentableName(),
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(type)?.toSourceSetDependent()?.toAnnotations()
                )
            )

            is KaClassErrorType -> UnresolvedBound(type.toString())
            is KaFunctionType -> toFunctionalTypeConstructorFrom(type)
            is KaDynamicType -> Dynamic
            is KaDefinitelyNotNullType -> DefinitelyNonNullable(
                toBoundFrom(type.original, isJavaContext)
            )

            // Java platform types are represented as flexible types (e.g. String..String?).
            // For Java source types, unwrap to the lower bound (non-nullable version)
            // to match the behavior of the PSI-based Java translator.
            is KaFlexibleType -> toBoundFrom(type.lowerBound, isJavaContext)

            is KaErrorType -> UnresolvedBound(type.toString())
            is KaCapturedType -> throw NotImplementedError()
            is KaIntersectionType -> throw NotImplementedError()
            else -> throw NotImplementedError()
        }.let {
            asNullableIfMarked(type, it)
        }

    private fun KaSession.asNullableIfMarked(type: KaType, bound: Bound): Bound =
        if (type.isMarkedNullable) Nullable(bound) else bound


    fun KaSession.buildAncestryInformationFrom(
        type: KaType
    ): AncestryNode {
        val (interfaces, superclass) = type.directSupertypes(true).filterNot { it.isAnyType }
            .partition {
                val typeConstructorWithKind = toTypeConstructorWithKindFrom(it)
                typeConstructorWithKind.kind == KotlinClassKindTypes.INTERFACE ||
                        typeConstructorWithKind.kind == JavaClassKindTypes.INTERFACE
            }

        return AncestryNode(
            typeConstructor = toTypeConstructorWithKindFrom(type).typeConstructor,
            superclass = superclass.map { buildAncestryInformationFrom(it) }.singleOrNull(),
            interfaces = interfaces.map { buildAncestryInformationFrom(it) }
        )
    }

    internal fun KaSession.toTypeConstructorWithKindFrom(type: KaType): TypeConstructorWithKind = when (type) {
        is KaUsualClassType ->
            when (val classSymbol = type.symbol) {
                is KaNamedClassSymbol -> TypeConstructorWithKind(
                    toTypeConstructorFrom(type),
                    classSymbol.classKind.toDokkaClassKind(classSymbol.isJavaSource())
                )

                is KaAnonymousObjectSymbol -> throw NotImplementedError()
                is KaTypeAliasSymbol -> toTypeConstructorWithKindFrom(classSymbol.expandedType)
            }

        is KaClassErrorType -> TypeConstructorWithKind(
            GenericTypeConstructor(
                dri = DRI(packageName = "", classNames = "$ERROR_CLASS_NAME $type"),
                projections = emptyList(),

                ),
            KotlinClassKindTypes.CLASS
        )

        is KaErrorType -> TypeConstructorWithKind(
            GenericTypeConstructor(
                dri = DRI(packageName = "", classNames = "$ERROR_CLASS_NAME $type"),
                projections = emptyList(),

                ),
            KotlinClassKindTypes.CLASS
        )

        is KaFunctionType -> TypeConstructorWithKind(
            toFunctionalTypeConstructorFrom(type),
            KotlinClassKindTypes.CLASS
        )

        is KaDefinitelyNotNullType -> toTypeConstructorWithKindFrom(type.original)

        // Java platform types are represented as flexible types (e.g. String..String?)
        is KaFlexibleType -> toTypeConstructorWithKindFrom(type.lowerBound)

        is KaCapturedType -> throw NotImplementedError()
        is KaDynamicType -> throw NotImplementedError()
        is KaIntersectionType -> throw NotImplementedError()
        is KaTypeParameterType -> throw NotImplementedError()
        else -> throw NotImplementedError()
    }

    private fun KaSession.getDokkaAnnotationsFrom(annotated: KaAnnotated): List<Annotations.Annotation>? =
        with(annotationTranslator) { getAllAnnotationsFrom(annotated) }.takeUnless { it.isEmpty() }

    private fun KaClassKind.toDokkaClassKind(isJavaSource: Boolean = false): ClassKind = if (isJavaSource) {
        when (this) {
            KaClassKind.CLASS -> JavaClassKindTypes.CLASS
            KaClassKind.ENUM_CLASS -> JavaClassKindTypes.ENUM_CLASS
            KaClassKind.ANNOTATION_CLASS -> JavaClassKindTypes.ANNOTATION_CLASS
            KaClassKind.INTERFACE -> JavaClassKindTypes.INTERFACE
            // Java doesn't have objects, but just in case:
            KaClassKind.OBJECT, KaClassKind.COMPANION_OBJECT, KaClassKind.ANONYMOUS_OBJECT -> JavaClassKindTypes.CLASS
        }
    } else {
        when (this) {
            KaClassKind.CLASS -> KotlinClassKindTypes.CLASS
            KaClassKind.ENUM_CLASS -> KotlinClassKindTypes.ENUM_CLASS
            KaClassKind.ANNOTATION_CLASS -> KotlinClassKindTypes.ANNOTATION_CLASS
            KaClassKind.OBJECT -> KotlinClassKindTypes.OBJECT
            KaClassKind.COMPANION_OBJECT -> KotlinClassKindTypes.OBJECT
            KaClassKind.INTERFACE -> KotlinClassKindTypes.INTERFACE
            KaClassKind.ANONYMOUS_OBJECT -> KotlinClassKindTypes.OBJECT
        }
    }

    private fun KaSymbol.isJavaSource() = isJavaOrigin()

    companion object {
        /** Kotlin primitive types → Java primitive type names */
        private val kotlinPrimitiveToJava = mapOf(
            "kotlin.Int" to "int",
            "kotlin.Long" to "long",
            "kotlin.Short" to "short",
            "kotlin.Byte" to "byte",
            "kotlin.Char" to "char",
            "kotlin.Boolean" to "boolean",
            "kotlin.Float" to "float",
            "kotlin.Double" to "double",
        )

        /** Kotlin primitive array types → Java primitive type names */
        private val kotlinPrimitiveArrayToJava = mapOf(
            "kotlin.IntArray" to "int",
            "kotlin.LongArray" to "long",
            "kotlin.ShortArray" to "short",
            "kotlin.ByteArray" to "byte",
            "kotlin.CharArray" to "char",
            "kotlin.BooleanArray" to "boolean",
            "kotlin.FloatArray" to "float",
            "kotlin.DoubleArray" to "double",
        )
    }
}
