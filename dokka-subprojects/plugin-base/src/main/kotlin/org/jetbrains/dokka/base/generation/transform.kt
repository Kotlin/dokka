/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.kotlin.documentation.*

// TODO: sorting
internal fun DModule.toKdModule(): KdModule {
    return KdModule(
        name = name,
        fragments = sourceSets.map { sourceSet ->
            KdFragment(
                name = sourceSet.sourceSetID.sourceSetName, // TODO: name vs displayName
                dependsOn = sourceSet.dependentSourceSets.map { it.sourceSetName },
                targets = listOf(
                    KdTarget.JVM // TODO based on platform for now
                ),
                packages = packages.map {
                    it.toKdPackage(sourceSet)
                },
                documentation = null, // based on module.documentation[sourceSet]
            )
        }
    )
}

private fun DPackage.toKdPackage(sourceSet: DokkaConfiguration.DokkaSourceSet): KdPackage {
    return KdPackage(
        name = name,
        declarations = functions.map { it.toKdFunction(sourceSet) },
//                properties.map { it.toKdProperty(sourceSet) } +
//                classlikes.map { it.toKdClassifier(sourceSet) } +
//                typealiases.map { it.toKdClassifier(sourceSet) },
        documentation = null,
    )
}

@OptIn(ExperimentalDokkaApi::class)
private fun DFunction.toKdFunction(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
): KdFunction {
    val extraModifiers = extra[AdditionalModifiers]?.content?.get(sourceSet).orEmpty()
    // TODO: ignore file level annotations?
    val annotations = extra[Annotations]?.directAnnotations?.get(sourceSet).orEmpty()

    val docs = documentation[sourceSet]?.children.orEmpty()

    return KdFunction(
        name = name,
        returns = KdReturns(
            type = type.toKdType(),
            documentation = docs.firstNotNullOfOrNull { it as? Return }?.root?.toKdDocumentation()
        ),

        isSuspend = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.Suspend),
        isOperator = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.Operator),
        isInfix = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.Infix),
        isInline = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.Inline),
        isTailRec = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.TailRec),
        isStatic = extraModifiers.contains(ExtraModifiers.JavaOnlyModifiers.Static), // TODO? is it enough?

        receiverParameter = receiver?.toKdReceiverParameter(sourceSet),
        valueParameters = parameters.map { it.toKdValueParameter(sourceSet) },
        contextParameters = contextParameters.map { it.toKdContextParameter(sourceSet) },

        throws = docs.filterIsInstance<Throws>().map {
            KdThrows(
                classifierId = it.exceptionAddress?.toKdClassifierId() ?: TODO(""),
                documentation = it.root.toKdDocumentation()
            )
        },
        sourceLanguage = KdSourceLanguage.KOTLIN, // TODO: not enought information right now
        visibility = visibility[sourceSet]!!.toKdVisibility(),
        modality = modifier[sourceSet]!!.toKdModality(),
        actuality = kdActuality(sourceSet),
        isExternal = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.External),
        annotations = annotations.mapNotNull(Annotations.Annotation::toKdAnnotation)
//        typeParameters = typeParameters.map { it.toKdTypeParameter() }
    )
}

private fun Bound.toKdType(): KdType {
    return when (this) {
        is DefinitelyNonNullable -> TODO()
        Dynamic -> TODO()
        is JavaObject -> TODO()
        is Nullable -> TODO()
        is PrimitiveJavaType -> TODO()
        is TypeAliased -> TODO()
        is FunctionalTypeConstructor -> TODO()
        is GenericTypeConstructor -> TODO()
        is TypeParameter -> TODO()
        is UnresolvedBound -> TODO()
        Void -> TODO()
    }
}

private fun Visibility.toKdVisibility(): KdVisibility = when (this) {
    JavaVisibility.Default -> KdVisibility.PACKAGE_PRIVATE
    JavaVisibility.Private -> KdVisibility.PRIVATE
    JavaVisibility.Protected -> KdVisibility.PACKAGE_PROTECTED
    JavaVisibility.Public -> KdVisibility.PUBLIC

    KotlinVisibility.Internal -> KdVisibility.INTERNAL
    KotlinVisibility.Private -> KdVisibility.PRIVATE
    KotlinVisibility.Protected -> KdVisibility.PROTECTED
    KotlinVisibility.Public -> KdVisibility.PUBLIC
}

private fun Modifier.toKdModality(): KdModality = when (this) {
    JavaModifier.Abstract -> KdModality.ABSTRACT
    JavaModifier.Empty -> KdModality.OPEN
    JavaModifier.Final -> KdModality.FINAL
    KotlinModifier.Abstract -> KdModality.ABSTRACT
    KotlinModifier.Empty -> KdModality.FINAL
    KotlinModifier.Final -> KdModality.FINAL
    KotlinModifier.Open -> KdModality.OPEN
    KotlinModifier.Sealed -> KdModality.SEALED
}

private fun <T> T.kdActuality(
    sourceSet: DokkaConfiguration.DokkaSourceSet
): KdActuality? where T : Documentable, T : WithIsExpectActual = when {
    !isExpectActual -> null
    expectPresentInSet == sourceSet -> KdActuality.EXPECT
    else -> KdActuality.ACTUAL
}

private fun Annotations.Annotation.toKdAnnotation(): KdAnnotation? {
    if (!mustBeDocumented) return null

    return KdAnnotation(
        classifierId = dri.toKdClassifierId(),
        useSiteTargets = emptyList(), // TODO
        arguments = params.map { (key, value) ->
            KdAnnotationArgument(
                name = key,
                value = when (value) {
                    is AnnotationValue -> TODO()
                    is ArrayValue -> TODO()
                    is ClassValue -> KdAnnotationArgumentValue.Class(value.classDRI.toKdClassifierId())
                    is EnumValue -> KdAnnotationArgumentValue.Enum(value.enumDri.toKdCallableId())
                    // TODO
                    is LiteralValue -> KdAnnotationArgumentValue.Const(
                        KdConstValue(value.text())
                    )
                }
            )
        }
    )
}

private fun DParameter.toKdReceiverParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdReceiverParameter {
    return KdReceiverParameter(
        type = type.toKdType(),
        documentation = documentation[sourceSet]?.children?.firstNotNullOfOrNull { it as? Receiver }?.root?.toKdDocumentation()
    )
}

private fun DParameter.toKdValueParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdValueParameter {
    val extraModifiers = extra[AdditionalModifiers]?.content?.get(sourceSet).orEmpty()
    return KdValueParameter(
        name = name ?: "UKNOWN_PARAMETER_NAME",
        type = type.toKdType(),
        defaultValue = when (val expr = extra[DefaultValue]?.expression?.get(sourceSet)) {
            is IntegerConstant -> expr.value.toString()
            is FloatConstant -> expr.value.toString() + "f"
            is DoubleConstant -> expr.value.toString()
            is BooleanConstant -> expr.value.toString()
            is StringConstant -> "\"${expr.value}\""
            is ComplexExpression -> expr.value
            else -> null
        }?.let {
            KdParameterDefaultValue.Const(KdConstValue(it))
        },
        isNoinline = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.NoInline),
        isCrossinline = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.CrossInline),
        isVararg = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.VarArg),
        documentation = null, // TODO: what is the correct way to get docs
    )
}

private fun DParameter.toKdContextParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdContextParameter {
    return KdContextParameter(
        name = name,
        type = type.toKdType(),
        documentation = null, // TODO: what is the correct way to get docs
    )
}

// TODO: throw an error here?
private fun DRI.toKdClassifierId(): KdClassifierId = KdClassifierId(
    packageName ?: "UNKNOWN_PACKAGE_NAME",
    classNames ?: "UNKNOWN_CLASS_NAME"
)

private fun DRI.toKdCallableId(): KdCallableId = KdCallableId(
    packageName ?: "UNKNOWN_PACKAGE_NAME",
    classNames ?: "UNKNOWN_CLASS_NAME",
    callable?.name ?: "UNKNOWN_CALLABLE_NAME",
    false // TODO
)

private fun DocTag.toKdDocumentation(): KdDocumentation = when (this) {
    is A -> TODO()
    is B -> TODO()
    is Big -> TODO()
    is BlockQuote -> TODO()
    Br -> TODO()
    is Caption -> TODO()
    is Cite -> TODO()
    is CodeBlock -> TODO()
    is CodeInline -> TODO()
    is CustomDocTag -> TODO()
    is Dd -> TODO()
    is Dfn -> TODO()
    is Dir -> TODO()
    is Div -> TODO()
    is Dl -> TODO()
    is DocumentationLink -> TODO()
    is Dt -> TODO()
    is Em -> TODO()
    is Font -> TODO()
    is Footer -> TODO()
    is Frame -> TODO()
    is FrameSet -> TODO()
    is H1 -> TODO()
    is H2 -> TODO()
    is H3 -> TODO()
    is H4 -> TODO()
    is H5 -> TODO()
    is H6 -> TODO()
    is Head -> TODO()
    is Header -> TODO()
    HorizontalRule -> TODO()
    is Html -> TODO()
    is I -> TODO()
    is IFrame -> TODO()
    is Img -> TODO()
    is Index -> TODO()
    is Input -> TODO()
    is Li -> TODO()
    is Link -> TODO()
    is Listing -> TODO()
    is Main -> TODO()
    is Menu -> TODO()
    is Meta -> TODO()
    is Nav -> TODO()
    is NoFrames -> TODO()
    is NoScript -> TODO()
    is Ol -> TODO()
    is P -> TODO()
    is Pre -> TODO()
    is Script -> TODO()
    is Section -> TODO()
    is Small -> TODO()
    is Span -> TODO()
    is Strikethrough -> TODO()
    is Strong -> TODO()
    is Sub -> TODO()
    is Sup -> TODO()
    is TBody -> TODO()
    is TFoot -> TODO()
    is THead -> TODO()
    is Table -> TODO()
    is Td -> TODO()
    is Text -> TODO()
    is Th -> TODO()
    is Title -> TODO()
    is Tr -> TODO()
    is Tt -> TODO()
    is U -> TODO()
    is Ul -> TODO()
    is Var -> TODO()
}