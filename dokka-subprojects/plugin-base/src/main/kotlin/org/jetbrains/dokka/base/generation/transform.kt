/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DRIExtraContainer
import org.jetbrains.dokka.links.EnumEntryDRIExtra
import org.jetbrains.dokka.links.PointingToCallableParameters
import org.jetbrains.dokka.links.PointingToContextParameters
import org.jetbrains.dokka.links.PointingToDeclaration
import org.jetbrains.dokka.links.PointingToGenericParameters
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.kotlin.documentation.*

// TODO: sorting
internal fun DModule.toKdModule(): KdModule {

    return KdModule(
        name = name,
        fragments = sourceSets.map { sourceSet ->
            val docs = documentation[sourceSet]?.children.orEmpty()
            require(docs.filterNot { it is Description }.isEmpty()) {
                "Documentation contains wrong nodes: ${docs.filterNot { it is Description }}"
            }
            KdFragment(
                name = sourceSet.sourceSetID.sourceSetName, // TODO: name vs displayName
                dependsOn = sourceSet.dependentSourceSets.map { it.sourceSetName },
                targets = emptyList(), // TODO: targets/platforms
                packages = packages.mapNotNull { it.toKdPackage(sourceSet) },
                documentation = docs.filterIsInstance<Description>().singleOrNullIfEmpty().toKdDocumentation(),
            )
        }
    )
}

private fun DPackage.toKdPackage(sourceSet: DokkaConfiguration.DokkaSourceSet): KdPackage? {
    // TODO?
    if (!sourceSets.contains(sourceSet)) return null

    val docs = documentation[sourceSet]?.children.orEmpty()

    println(docs)

    return KdPackage(
        name = name,
        declarations = functions.mapNotNull { it.toKdFunction(sourceSet) },
//                properties.map { it.toKdProperty(sourceSet) } +
//                classlikes.map { it.toKdClassifier(sourceSet) } +
//                typealiases.map { it.toKdClassifier(sourceSet) },
        documentation = emptyList(),
    ).takeIf { it.declarations.isNotEmpty() }
}

@OptIn(ExperimentalDokkaApi::class)
private fun DFunction.toKdFunction(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
): KdFunction? {
    if (!sourceSets.contains(sourceSet)) return null

    val extraModifiers = extra[AdditionalModifiers]?.content?.get(sourceSet).orEmpty()
    // TODO: ignore file level annotations?
    val annotations = extra[Annotations]?.directAnnotations?.get(sourceSet).orEmpty()

    val docs = documentation[sourceSet]?.children.orEmpty()

    require(docs.filterNot {
        it is Description || it is Return || it is Throws || it is Param || it is Sample // TODO: samples
    }.isEmpty()) {
        "Documentation contains wrong nodes: ${docs.filterNot { it is Description }}"
    }

    return KdFunction(
        name = name,
        returns = KdReturns(
            type = type.toKdType(),
            documentation = docs.filterIsInstance<Return>().singleOrNullIfEmpty().toKdDocumentation()
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
        typeParameters = generics.map { it.toKdTypeParameter(sourceSet) },

        throws = docs.filterIsInstance<Throws>().map {
            KdThrows(
                classifierId = it.exceptionAddress?.toKdClassifierId() ?: TODO(""),
                documentation = it.toKdDocumentation()
            )
        },
        sourceLanguage = KdSourceLanguage.KOTLIN, // TODO: not enought information right now
        visibility = kdVisibility(sourceSet),
        modality = kdModality(sourceSet),
        actuality = kdActuality(sourceSet),
        isExternal = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.External),
        annotations = annotations.mapNotNull(Annotations.Annotation::toKdAnnotation),
        documentation = docs.filterIsInstance<Description>().singleOrNullIfEmpty().toKdDocumentation(),
    )
}

private fun WithVisibility.kdVisibility(sourceSet: DokkaConfiguration.DokkaSourceSet): KdVisibility {
    return when (visibility[sourceSet]) {
        JavaVisibility.Default -> KdVisibility.PACKAGE_PRIVATE
        JavaVisibility.Private -> KdVisibility.PRIVATE
        JavaVisibility.Protected -> KdVisibility.PACKAGE_PROTECTED
        JavaVisibility.Public -> KdVisibility.PUBLIC

        KotlinVisibility.Internal -> KdVisibility.INTERNAL
        KotlinVisibility.Private -> KdVisibility.PRIVATE
        KotlinVisibility.Protected -> KdVisibility.PROTECTED
        KotlinVisibility.Public -> KdVisibility.PUBLIC
        null -> KdVisibility.PUBLIC // safe default?
    }
}

private fun WithAbstraction.kdModality(sourceSet: DokkaConfiguration.DokkaSourceSet): KdModality {
    return when (modifier[sourceSet]) {
        JavaModifier.Abstract -> KdModality.ABSTRACT
        JavaModifier.Empty -> KdModality.OPEN
        JavaModifier.Final -> KdModality.FINAL
        KotlinModifier.Abstract -> KdModality.ABSTRACT
        KotlinModifier.Empty -> KdModality.FINAL
        KotlinModifier.Final -> KdModality.FINAL
        KotlinModifier.Open -> KdModality.OPEN
        KotlinModifier.Sealed -> KdModality.SEALED
        null -> KdModality.FINAL // safe default?
    }
}

private fun <T> T.kdActuality(
    sourceSet: DokkaConfiguration.DokkaSourceSet
): KdActuality? where T : Documentable, T : WithIsExpectActual = when {
    !isExpectActual -> null
    expectPresentInSet == sourceSet -> KdActuality.EXPECT
    else -> KdActuality.ACTUAL
}

private fun Annotations.Annotation.toKdAnnotation(
    mustBeDocumented: Boolean = this.mustBeDocumented,
): KdAnnotation? {
    if (!mustBeDocumented) return null

    fun AnnotationParameterValue.toKdAnnotationArgumentValue(): KdAnnotationArgumentValue {
        return when (this) {
            is AnnotationValue -> KdAnnotationArgumentValue.Annotation(annotation.toKdAnnotation(mustBeDocumented = true)!!)
            is ArrayValue -> KdAnnotationArgumentValue.Array(value.map { it.toKdAnnotationArgumentValue() })
            is ClassValue -> KdAnnotationArgumentValue.Class(classDRI.toKdClassifierId())
            is EnumValue -> KdAnnotationArgumentValue.Enum(enumDri.toKdCallableId())
            // TODO
            is LiteralValue -> KdAnnotationArgumentValue.Const(KdConstValue(text()))
        }
    }

    return KdAnnotation(
        classifierId = dri.toKdClassifierId(),
        useSiteTargets = emptyList(), // TODO
        arguments = params.map { (key, value) ->
            KdAnnotationArgument(
                name = key,
                value = value.toKdAnnotationArgumentValue()
            )
        }
    )
}

private fun DParameter.toKdReceiverParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdReceiverParameter {
    val docs = documentation[sourceSet]?.children.orEmpty()

    require(docs.filterNot { it is Receiver }.isEmpty()) {
        "Documentation contains wrong nodes: ${docs.filterNot { it is Receiver }}"
    }

    return KdReceiverParameter(
        type = type.toKdType(),
        documentation = docs.filterIsInstance<Receiver>().singleOrNullIfEmpty().toKdDocumentation()
    )
}

private fun DParameter.toKdValueParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdValueParameter {
    val extraModifiers = extra[AdditionalModifiers]?.content?.get(sourceSet).orEmpty()
    val docs = documentation[sourceSet]?.children.orEmpty()

    require(docs.filterNot { it is Param }.isEmpty()) {
        "Documentation contains wrong nodes: ${docs.filterNot { it is Param }}"
    }

    return KdValueParameter(
        name = requireNotNull(name) { "Parameter $this does not have a name" },
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
        documentation = docs.filterIsInstance<Param>().singleOrNullIfEmpty().toKdDocumentation(),
    )
}

private fun DParameter.toKdContextParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdContextParameter {
    val docs = documentation[sourceSet]?.children.orEmpty()

    require(docs.filterNot { it is Param }.isEmpty()) {
        "Documentation contains wrong nodes: ${docs.filterNot { it is Param }}"
    }

    return KdContextParameter(
        name = name,
        type = type.toKdType(),
        documentation = docs.filterIsInstance<Param>().singleOrNullIfEmpty().toKdDocumentation(),
    )
}

private fun DTypeParameter.toKdTypeParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdTypeParameter {
    val extraModifiers = extra[AdditionalModifiers]?.content?.get(sourceSet).orEmpty()
    val docs = documentation[sourceSet]?.children.orEmpty()

    require(docs.filterNot { it is Param }.isEmpty()) {
        "Documentation contains wrong nodes: ${docs.filterNot { it is Param }}"
    }

    return KdTypeParameter(
        name = name,
        upperBounds = bounds.map { it.toKdType() },
        variance = when (variantTypeParameter) {
            is Invariance<*> -> null
            is Contravariance<*> -> KdVariance.IN
            is Covariance<*> -> KdVariance.OUT
        },
        isReified = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.Reified),
        documentation = docs.filterIsInstance<Param>().singleOrNullIfEmpty().toKdDocumentation(),
    )
}

private fun Projection.toKdTypeArgument(): KdTypeProjection {
    return when (this) {
        is Star -> KdTypeProjection(null, null)
        is Variance<*> -> KdTypeProjection(
            type = inner.toKdType(),
            variance = when (this) {
                is Invariance<*> -> null
                is Contravariance<*> -> KdVariance.IN
                is Covariance<*> -> KdVariance.OUT
            }
        )

        is Bound -> TODO("should not happen?: $this")
    }
}

@OptIn(ExperimentalDokkaApi::class)
private fun Bound.toKdType(nullability: KdNullability = KdNullability.NOT_NULLABLE): KdType = when (this) {
    is GenericTypeConstructor -> {
        KdClassifierType(
            classifierId = dri.toKdClassifierId(),
            typeArguments = projections.map { it.toKdTypeArgument() },
            nullability = nullability
        )
    }

    is FunctionalTypeConstructor -> {
        // projections contain in the order:
        // - context parameters
        val contextParameterTypes = projections.subList(0, contextParametersCount).map(Projection::toKdTypeArgument)
        // - receiver
        val receiverType = if (isExtensionFunction) {
            projections[contextParametersCount].toKdTypeArgument()
        } else null
        // - value parameters
        val valueParameterTypes = projections.subList(
            contextParametersCount + (if (isExtensionFunction) 1 else 0),
            projections.size - 1 // excluding last
        ).map(Projection::toKdTypeArgument)
        // - return type
        val returnType = projections.last().toKdTypeArgument()

        KdFunctionalType(
            returnType = returnType,
            receiverType = receiverType,
            valueParameterTypes = valueParameterTypes,
            contextParameterTypes = contextParameterTypes,
            isSuspend = isSuspendable,
            nullability = nullability
        )
    }

    is TypeParameter -> {
        KdTypeParameterType(
            name = name,
            nullability = nullability
        )
    }

    Dynamic -> KdDynamicType

    // TODO: create constants for java types?
    Void -> KdClassifierType(
        classifierId = KdClassifierId("kotlin", "Unit"),
        typeArguments = emptyList(),
        nullability = nullability
    )

    is JavaObject -> KdClassifierType(
        classifierId = KdClassifierId("kotlin", "Any"),
        typeArguments = emptyList(),
        nullability = nullability
    )

    is PrimitiveJavaType -> KdClassifierType(
        // TODO: recheck conversion
        classifierId = KdClassifierId("kotlin", name.replaceFirstChar(Char::uppercase)),
        typeArguments = emptyList(),
        nullability = nullability
    )

    is DefinitelyNonNullable -> inner.toKdType(KdNullability.DEFINITELY_NOT_NULLABLE)
    is Nullable -> inner.toKdType(KdNullability.NULLABLE)

    is TypeAliased -> KdUnresolvedType("TypeAliased: $this", nullability)
    // TODO: K2 impl is a bit wrong here
    is UnresolvedBound -> KdUnresolvedType(name, nullability)
}
//
//private inline fun <reified T : TagWrapper> DocumentationNode.toKdDocumentation(
//
//): KdDocumentation {
//    children.filterIsInstance<T>()
//    return KdDocumentation(children.map { it.toKdDocumentationNode() })
//}

private fun TagWrapper?.toKdDocumentation(): List<KdDocumentationNode> {
    if (this == null) return emptyList()
    val root = requireNotNull(root as? CustomDocTag) { "Only CustomDocTag is supported: $this" }
    require(root.name == "MARKDOWN_FILE") { "Only MARKDOWN_FILE is supported" }
    require(root.params.isEmpty()) { "Params are not supported: $root.params in $this" }

    return children.map(DocTag::toKdDocumentationNode)
}

private fun DocTag.toKdDocumentationNode(): KdDocumentationNode = when (this) {
    is CustomDocTag -> error("SHOULD NOT HAPPEN!")

    is P -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        KdDocumentationNode.Paragraph(children.map(DocTag::toKdDocumentationNode))
    }

    is Text -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        require(children.isEmpty()) { "Children are not supported: $children in $this" }
        KdDocumentationNode.Text(body)
    }

    is CodeInline -> KdDocumentationNode.CodeInline(
        text = buildString {
            children.forEach {
                when (it) {
                    is Text -> append(it.body)
                    else -> error("WTF?")
                }
            }
        },
        language = params["lang"] ?: "kotlin"
    )

    is CodeBlock -> KdDocumentationNode.CodeBlock(
        lines = buildString {
            children.forEach {
                when (it) {
                    is Text -> append(it.body)
                    is Br -> appendLine()
                    else -> error("should not happen: $it")
                }
            }
        }.split('\n'),
        language = params["lang"] ?: "kotlin"
    )

    is A -> KdDocumentationNode.ExternalLink(
        label = children.map(DocTag::toKdDocumentationNode),
        url = params.getValue("href")
    )

    // bold, strike through...

    is DocumentationLink -> KdDocumentationNode.Link(
        label = children.map(DocTag::toKdDocumentationNode),
        reference = dri.toKdLinkReference()
    )

    else -> KdDocumentationNode.Text("UNKNOWN: $this")
}

@OptIn(ExperimentalDokkaApi::class)
private fun DRI.toKdLinkReference(): KdLinkReference {
    return when (val target = target) {
        is PointingToCallableParameters -> KdLinkReference.ValueParameter(target.parameterIndex)
        is PointingToContextParameters -> KdLinkReference.ContextParameter(target.parameterIndex)
        is PointingToGenericParameters -> KdLinkReference.TypeParameter(target.parameterIndex)
        PointingToDeclaration -> when (callable) {
            null if classNames == null -> KdLinkReference.Package(packageName ?: "UNKNOWN_PACKAGE_NAME")
            null -> KdLinkReference.Classifier(toKdClassifierId())
            else -> KdLinkReference.Callable(toKdCallableId())
        }
    }
}

// TODO: throw an error here?
private fun DRI.toKdClassifierId(): KdClassifierId = KdClassifierId(
    packageName = requireNotNull(packageName) { "packageName is null for $this" },
    classNames = requireNotNull(classNames) { "classNames is null for $this" },
)

private fun DRI.toKdCallableId(): KdCallableId {
    val packageName = requireNotNull(packageName) { "packageName is null for $this" }

    // enum entry
    return if (extra != null && DRIExtraContainer(extra)[EnumEntryDRIExtra] != null) {
        val pseudoClassNames = requireNotNull(classNames) { "classNames is null for $this" }
        val classNames = pseudoClassNames.substringBeforeLast('.', "")
        val callableName = pseudoClassNames.substringAfterLast('.', "")
        require(classNames.isNotBlank()) { "classNames is blank for $this" }
        require(callableName.isNotBlank()) { "callableName is blank for $this" }

        KdCallableId(
            packageName = packageName,
            classNames = classNames,
            callableName = callableName
        )
    } else {
        val callable = requireNotNull(callable) { "callable is null for $this" }
        KdCallableId(
            packageName = packageName,
            classNames = classNames,
            callableName = callable.name
        )
    }
}

private fun <T : Any> List<T>.singleOrNullIfEmpty(): T? = when (size) {
    0 -> null
    else -> single()
}
