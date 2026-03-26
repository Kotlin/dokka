/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation.kdp

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.kotlin.documentation.*
import java.io.File
import kotlin.time.measureTimedValue

internal fun saveModule(
    dModule: DModule,
    outputDirectory: File
) {
    fun <T> measured(tag: String, block: () -> T): Result<T> {
        val (result, duration) = measureTimedValue { runCatching { block() } }
        result.onSuccess { println("$tag: $duration") }
        result.onFailure {
            println("[FAILED] $tag: $duration")
            it.printStackTrace()
        }
        return result
    }

    val kdModule = measured("transform") { dModule.toKdModule() }.getOrThrow()

    measured("coverage") { kdModule.calculateCoverage() }.getOrThrow()

    with(outputDirectory.resolve("kdp")) {
        mkdirs()
        measured("json") { resolve("${kdModule.name}.json").writeText(kdModule.encodeToJson(prettyPrint = false)) }
        measured("pretty-json") { resolve("${kdModule.name}-pretty.json").writeText(kdModule.encodeToJson(prettyPrint = true)) }
        // json is small enough when zipped
//        measured("cbor") { resolve("${kdModule.name}.cbor").writeBytes(kdModule.encodeToCbor()) }
//        measured("pb-schema") { resolve("${kdModule.name}.schema").writeText(protoSchema()) }
//        measured("pb") { resolve("${kdModule.name}.pb").writeBytes(kdModule.encodeToProtoBuf()) }
    }
}

// TODO: sorting
private fun DModule.toKdModule(): KdModule = KdModule(
    name = name,
    fragments = sourceSets.map { sourceSet ->
        val tagWrappers = tagWrappers(sourceSet) { it is Description }
        KdFragment(
            name = sourceSet.sourceSetID.sourceSetName, // TODO: name vs displayName
            dependsOn = sourceSet.dependentSourceSets.map { it.sourceSetName },
            targets = emptyList(), // TODO: targets/platforms
            packages = packages.mapNotNull { it.toKdPackage(sourceSet) },
            documentation = tagWrappers.filterIsInstance<Description>().singleOrNullIfEmpty().toKdDocumentation(),
        )
    }
)

private fun DPackage.toKdPackage(
    sourceSet: DokkaConfiguration.DokkaSourceSet
): KdPackage? {
    if (!sourceSets.contains(sourceSet)) return null

    val declarations = buildList {
        functions.mapNotNullTo(this) { it.toKdFunction(sourceSet) }
        properties.mapNotNullTo(this) { it.toKdVariable(sourceSet) }
        classlikes.mapNotNullTo(this) { it.toKdClass(sourceSet) }
        typealiases.mapNotNullTo(this) { it.toKdTypealias(sourceSet) }
    }

    if (declarations.isEmpty()) return null

    val tagWrappers = tagWrappers(sourceSet) { it is Description }

    return KdPackage(
        name = name,
        declarations = declarations,
        documentation = tagWrappers.filterIsInstance<Description>().singleOrNullIfEmpty().toKdDocumentation(),
    )
}

@OptIn(ExperimentalDokkaApi::class)
private fun DProperty.toKdVariable(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
): KdVariable? {
    if (!sourceSets.contains(sourceSet)) return null

    val extraModifiers = extraModifiers(sourceSet)
    val annotations = directAnnotations(sourceSet)
    val tagWrappers = tagWrappers(sourceSet) {
        it is Description || it is Param || it is Receiver || it is Return || it is Throws || it is Sample || it is See
    }

    return KdVariable(
        name = name,
        returns = KdReturns(
            type = type.toKdType(),
            documentation = tagWrappers.filterIsInstance<Return>().singleOrNullIfEmpty().toKdDocumentation()
        ),
        variableKind = KdVariableKind.PROPERTY, // TODO: java fields?

        isMutable = extra[IsVar] != null || setter != null,
        // TODO: same as in annotations
        constValue = extra[DefaultValue]?.expression?.get(sourceSet)?.toString()?.let(::KdConstValue),
        isStatic = extraModifiers.contains(ExtraModifiers.JavaOnlyModifiers.Static), // TODO? is it enough?

        receiverParameter = receiver?.toKdReceiverParameter(sourceSet),
        contextParameters = contextParameters.map { it.toKdContextParameter(sourceSet) },
        typeParameters = generics.map { it.toKdTypeParameter(sourceSet) },

        throws = tagWrappers.filterIsInstance<Throws>().map {
            KdThrows(
                // null means unresolved type - TBD what to do here
                classifierId = it.exceptionAddress?.toKdClassifierId() ?: error("should not happen: $it"),
                documentation = it.toKdDocumentation()
            )
        },
        sourceLanguage = KdSourceLanguage.KOTLIN, // TODO: not enought information right now
        visibility = kdVisibility(sourceSet),
        modality = kdModality(sourceSet),
        actuality = kdActuality(sourceSet),
        isExternal = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.External),
        annotations = annotations.mapNotNull(Annotations.Annotation::toKdAnnotation),
        documentation = tagWrappers.filterIsInstance<Description>().singleOrNullIfEmpty().toKdDocumentation(),
    )
}

@OptIn(ExperimentalDokkaApi::class)
private fun DEnumEntry.toKdVariable(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
    enum: DEnum
): KdVariable? {
    if (!sourceSets.contains(sourceSet)) return null

    val annotations = directAnnotations(sourceSet)
    val tagWrappers = tagWrappers(sourceSet) { it is Description }

    return KdVariable(
        name = name,
        // TODO: recheck type
        returns = KdReturns(
            type = KdClassifierType(enum.dri.toKdClassifierId()),
            documentation = tagWrappers.filterIsInstance<Return>().singleOrNullIfEmpty().toKdDocumentation()
        ),
        variableKind = KdVariableKind.ENUM_ENTRY,

        isMutable = false,
        constValue = null,
        isStatic = true,

        receiverParameter = null,
        contextParameters = emptyList(),
        typeParameters = emptyList(),

        throws = tagWrappers.filterIsInstance<Throws>().map {
            KdThrows(
                // null means unresolved type - TBD what to do here
                classifierId = it.exceptionAddress?.toKdClassifierId() ?: error("should not happen: $it"),
                documentation = it.toKdDocumentation()
            )
        },
        sourceLanguage = KdSourceLanguage.KOTLIN, // TODO: not enought information right now
        visibility = enum.kdVisibility(sourceSet),
        modality = KdModality.FINAL,
        actuality = enum.kdActuality(sourceSet),
        isExternal = false,
        annotations = annotations.mapNotNull(Annotations.Annotation::toKdAnnotation),
        documentation = tagWrappers.filterIsInstance<Description>().singleOrNullIfEmpty().toKdDocumentation(),
    )
}

@OptIn(ExperimentalDokkaApi::class)
private fun DFunction.toKdFunction(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
): KdFunction? {
    if (!sourceSets.contains(sourceSet)) return null

    val extraModifiers = extraModifiers(sourceSet)
    val annotations = directAnnotations(sourceSet)
    val tagWrappers = tagWrappers(sourceSet) {
        it is Description || it is Param || it is Receiver || it is Return || it is Throws || it is Sample || it is See
    }

    return KdFunction(
        name = name,
        returns = KdReturns(
            type = type.toKdType(),
            documentation = tagWrappers.filterIsInstance<Return>().singleOrNullIfEmpty().toKdDocumentation()
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

        throws = tagWrappers.filterIsInstance<Throws>().map {
            KdThrows(
                // null means unresolved type - TBD what to do here
                classifierId = it.exceptionAddress?.toKdClassifierId() ?: KdClassifierId(
                    packageName = "UNKNOWN",
                    classNames = it.name
                ),
                documentation = it.toKdDocumentation()
            )
        },
        sourceLanguage = KdSourceLanguage.KOTLIN, // TODO: not enought information right now
        visibility = kdVisibility(sourceSet),
        modality = kdModality(sourceSet),
        actuality = kdActuality(sourceSet),
        isExternal = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.External),
        annotations = annotations.mapNotNull(Annotations.Annotation::toKdAnnotation),
        documentation = tagWrappers.filterIsInstance<Description>().singleOrNullIfEmpty().toKdDocumentation(),
    )
}

@OptIn(ExperimentalDokkaApi::class)
private fun DFunction.toKdConstructor(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
): KdConstructor? {
    if (!sourceSets.contains(sourceSet)) return null

    val extraModifiers = extraModifiers(sourceSet)
    val annotations = directAnnotations(sourceSet)
    val tagWrappers = tagWrappers(sourceSet) {
        it is Description || it is Return || it is Throws || it is Param || it is Sample || it is See // TODO: support samples
    }

    return KdConstructor(
        name = name,
        returns = KdReturns(
            type = type.toKdType(),
            documentation = tagWrappers.filterIsInstance<Return>().singleOrNullIfEmpty().toKdDocumentation()
        ),

        isPrimary = extra[PrimaryConstructorExtra] != null,
        valueParameters = parameters.map { it.toKdValueParameter(sourceSet) },

        throws = tagWrappers.filterIsInstance<Throws>().map {
            KdThrows(
                // null means unresolved type - TBD what to do here
                classifierId = it.exceptionAddress?.toKdClassifierId() ?: error("should not happen: $it"),
                documentation = it.toKdDocumentation()
            )
        },
        sourceLanguage = KdSourceLanguage.KOTLIN, // TODO: not enought information right now
        visibility = kdVisibility(sourceSet),
        modality = kdModality(sourceSet),
        actuality = kdActuality(sourceSet),
        isExternal = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.External),
        annotations = annotations.mapNotNull(Annotations.Annotation::toKdAnnotation),
        documentation = tagWrappers.filterIsInstance<Description>().singleOrNullIfEmpty().toKdDocumentation(),
    )
}

@OptIn(ExperimentalDokkaApi::class)
private fun DClasslike.toKdClass(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
): KdClass? {
    if (!sourceSets.contains(sourceSet)) return null

    val extraModifiers = extraModifiers(sourceSet)
    val annotations = directAnnotations(sourceSet)

    val tagWrappers = tagWrappers(sourceSet) {
        it is Description || it is Param || it is Sample || it is See
    }

    return KdClass(
        name = requireNotNull(name) ?: error("Class name cannot be null: $this"),
        classKind = when (this) {
            is DClass -> KdClassKind.CLASS
            is DEnum -> KdClassKind.ENUM_CLASS
            is DAnnotation -> KdClassKind.ANNOTATION_CLASS
            is DObject -> KdClassKind.OBJECT
            is DInterface -> KdClassKind.INTERFACE
        },

        // TODO: isCompanion

        isData = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.Data),
        isValue = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.Value),
        isInner = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.Inner),

        superTypes = when (this) {
            is WithSupertypes -> supertypes[sourceSet].orEmpty().map { it.typeConstructor.toKdType() }
            else -> emptyList()
        },
        declarations = buildList {
            if (this@toKdClass is WithConstructors) constructors.mapNotNullTo(this) { it.toKdConstructor(sourceSet) }
            functions.mapNotNullTo(this) { it.toKdFunction(sourceSet) }
            properties.mapNotNullTo(this) { it.toKdVariable(sourceSet) }
            if (this@toKdClass is DEnum) entries.mapNotNullTo(this) { it.toKdVariable(sourceSet, this@toKdClass) }
            classlikes.mapNotNullTo(this) { it.toKdClass(sourceSet) }
            if (this@toKdClass is WithTypealiases) typealiases.mapNotNullTo(this) { it.toKdTypealias(sourceSet) }
        },
        typeParameters = when (this) {
            is WithGenerics -> generics.map { it.toKdTypeParameter(sourceSet) }
            else -> emptyList()
        },

        sourceLanguage = KdSourceLanguage.KOTLIN, // TODO: not enought information right now
        visibility = kdVisibility(sourceSet),
        modality = when (this) {
            is WithAbstraction -> kdModality(sourceSet)
            else -> KdModality.FINAL
        },
        actuality = kdActuality(sourceSet),
        isExternal = extraModifiers.contains(ExtraModifiers.KotlinOnlyModifiers.External),
        annotations = annotations.mapNotNull(Annotations.Annotation::toKdAnnotation),
        documentation = tagWrappers.filterIsInstance<Description>().singleOrNullIfEmpty().toKdDocumentation(),
    )
}

@OptIn(ExperimentalDokkaApi::class)
private fun DTypeAlias.toKdTypealias(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
): KdTypealias? {
    if (!sourceSets.contains(sourceSet)) return null

    val annotations = directAnnotations(sourceSet)
    val tagWrappers = tagWrappers(sourceSet) {
        it is Description || it is See
    }

    return KdTypealias(
        name = name,
        underlyingType = underlyingType.getValue(sourceSet).toKdType(),
        typeParameters = generics.map { it.toKdTypeParameter(sourceSet) },
        visibility = kdVisibility(sourceSet),
        actuality = null, // kdActuality(sourceSet), // TODO: there is a complex logic for this in Dokka...
        annotations = annotations.mapNotNull(Annotations.Annotation::toKdAnnotation),
        documentation = tagWrappers.filterIsInstance<Description>().singleOrNullIfEmpty().toKdDocumentation(),
    )
}

// TODO: support see tag