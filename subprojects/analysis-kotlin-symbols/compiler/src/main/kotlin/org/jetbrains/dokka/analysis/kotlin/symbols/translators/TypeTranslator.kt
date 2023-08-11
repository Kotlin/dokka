package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.*

internal class TypeTranslator(
    private val sourceSet: DokkaConfiguration.DokkaSourceSet,
    private val annotationTranslator: AnnotationTranslator
) {

    private fun <T> T.toSourceSetDependent() = if (this != null) mapOf(sourceSet to this) else emptyMap()

    private fun KtAnalysisSession.toProjection(typeProjection: KtTypeProjection): Projection =
        when (typeProjection) {
            is KtStarTypeProjection -> Star
            is KtTypeArgumentWithVariance -> toBoundFrom(typeProjection.type).wrapWithVariance(typeProjection.variance)
        }

    private fun KtAnalysisSession.toTypeConstructorFromTypeAliased(classType: KtUsualClassType): TypeAliased {
        val classSymbol = classType.classSymbol
        return if (classSymbol is KtTypeAliasSymbol)
            TypeAliased(
                typeAlias = GenericTypeConstructor(
                    dri = getDRIFromNonErrorClassType(classType),
                    projections = classType.ownTypeArguments.map { toProjection(it) }),
                inner = toBoundFrom(classSymbol.expandedType),
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(classType)?.toSourceSetDependent()?.toAnnotations()
                )
            ) else
            throw IllegalStateException("Expected type alias symbol in type")
    }

    private fun KtAnalysisSession.toTypeConstructorFrom(classType: KtUsualClassType) =
        GenericTypeConstructor(
            dri = getDRIFromNonErrorClassType(classType),
            projections = classType.ownTypeArguments.map { toProjection(it) },
            presentableName = classType.getPresentableName(),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(classType)?.toSourceSetDependent()?.toAnnotations()
            )
        )

    private fun KtAnalysisSession.toFunctionalTypeConstructorFrom(functionalType: KtFunctionalType) =
        FunctionalTypeConstructor(
            dri = getDRIFromNonErrorClassType(functionalType),
            projections = functionalType.ownTypeArguments.map { toProjection(it) },
            isExtensionFunction = functionalType.receiverType != null,
            isSuspendable = functionalType.isSuspend,
            presentableName = functionalType.getPresentableName(),
            extra = PropertyContainer.withAll(
                getDokkaAnnotationsFrom(functionalType)?.toSourceSetDependent()?.toAnnotations()
            )
        )

    internal fun KtAnalysisSession.toBoundFrom(type: KtType): Bound =
        when (type) {
            is KtUsualClassType -> {
                if (type.classSymbol is KtTypeAliasSymbol) toTypeConstructorFromTypeAliased(type)
                else toTypeConstructorFrom(type)
            }
            is KtTypeParameterType -> TypeParameter(
                dri = getDRIFromTypeParameter(type.symbol),
                name = type.name.asString(),
                presentableName = type.getPresentableName(),
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(type)?.toSourceSetDependent()?.toAnnotations()
                )
            )

            is KtClassErrorType -> UnresolvedBound(type.toString())
            is KtFunctionalType -> toFunctionalTypeConstructorFrom(type)
            is KtDynamicType -> Dynamic
            is KtDefinitelyNotNullType -> DefinitelyNonNullable(
                toBoundFrom(type.original)
            )

            is KtCapturedType -> TODO("Not yet implemented")
            is KtFlexibleType -> TypeAliased(
                toBoundFrom(type.upperBound),
                toBoundFrom(type.lowerBound),
                extra = PropertyContainer.withAll(
                    getDokkaAnnotationsFrom(type)?.toSourceSetDependent()?.toAnnotations()
                )
            )

            is KtIntegerLiteralType -> TODO("Not yet implemented")
            is KtIntersectionType -> TODO("Not yet implemented")
            is KtTypeErrorType -> UnresolvedBound(type.toString())
        }.let {
            if (type.isMarkedNullable) Nullable(it) else it
        }

    fun KtAnalysisSession.buildAncestryInformationFrom(
        type: KtType
    ): AncestryNode {
        val (interfaces, superclass) = type.getDirectSuperTypes().filterNot { it.isAny }
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

    internal fun KtAnalysisSession.toTypeConstructorWithKindFrom(type: KtType): TypeConstructorWithKind {
        try {
            return when (type) {
                is KtUsualClassType ->
                    when (val classSymbol = type.classSymbol) {
                        is KtNamedClassOrObjectSymbol -> TypeConstructorWithKind(
                            toTypeConstructorFrom(type),
                            classSymbol.classKind.toDokkaClassKind()
                        )
                        is KtAnonymousObjectSymbol -> TODO()
                        is KtTypeAliasSymbol -> toTypeConstructorWithKindFrom(classSymbol.expandedType)
                    }
                is KtClassErrorType -> TypeConstructorWithKind(
                    GenericTypeConstructor(
                        dri = DRI("[error]", "", null),
                        projections = emptyList(), // TODO: since 1.8.20 replace  `typeArguments ` with `ownTypeArguments`

                    ),
                    KotlinClassKindTypes.CLASS
                )
                is KtFunctionalType -> TypeConstructorWithKind(
                    toFunctionalTypeConstructorFrom(type),
                    KotlinClassKindTypes.CLASS
                )
                is KtDefinitelyNotNullType -> toTypeConstructorWithKindFrom(type.original)

//            is KtCapturedType -> TODO()
//            is KtDynamicType -> TODO()
//            is KtFlexibleType -> TODO()
//            is KtIntegerLiteralType -> TODO()
//            is KtIntersectionType -> TODO()
//            is KtTypeParameterType -> TODO()
                else -> throw IllegalStateException("Unknown ")
            }
        } catch (e: Throwable) {
            return TypeConstructorWithKind(
                GenericTypeConstructor(
                    dri = DRI("[error]", "", null),
                    projections = emptyList(), // TODO: since 1.8.20 replace  `typeArguments ` with `ownTypeArguments`

                ),
                KotlinClassKindTypes.CLASS
            )
            //throw IllegalStateException("Type error: ${type.asStringForDebugging()}" + type, e)
        }
    }
    private fun KtAnalysisSession.getDokkaAnnotationsFrom(annotated: KtAnnotated): List<Annotations.Annotation>? =
        with(annotationTranslator) { getAllAnnotationsFrom(annotated) }.takeUnless { it.isEmpty() }

    private fun KtClassKind.toDokkaClassKind() = when (this) {
        KtClassKind.CLASS -> KotlinClassKindTypes.CLASS
        KtClassKind.ENUM_CLASS -> KotlinClassKindTypes.ENUM_CLASS
        KtClassKind.ANNOTATION_CLASS -> KotlinClassKindTypes.ANNOTATION_CLASS
        KtClassKind.OBJECT -> KotlinClassKindTypes.OBJECT
        KtClassKind.COMPANION_OBJECT -> KotlinClassKindTypes.OBJECT
        KtClassKind.INTERFACE -> KotlinClassKindTypes.INTERFACE
        KtClassKind.ANONYMOUS_OBJECT -> KotlinClassKindTypes.OBJECT
    }
}