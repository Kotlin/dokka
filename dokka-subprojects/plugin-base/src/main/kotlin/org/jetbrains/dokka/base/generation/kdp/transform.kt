/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation.kdp

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.kotlin.documentation.*
import org.jetbrains.kotlin.documentation.KdClassKind
import java.io.File
import kotlin.time.measureTimedValue

internal fun saveModule(
    dModule: DModule,
    outputDirectory: File
) {
    val kdModule = dModule.toKdModule()

    with(outputDirectory.resolve("kdp")) {
        mkdirs()
        println("KDP: $absolutePath")
        fun runIt(block: () -> Unit) {
            val (result, duration) = measureTimedValue { runCatching { block() } }
            result.fold(
                onSuccess = {
                    println("Done in $duration")
                },
                onFailure = {
                    println("Failed in $duration")
                    it.printStackTrace()
                }
            )
        }

        runIt { resolve("${kdModule.name}.json").writeText(kdModule.encodeToJson(prettyPrint = false)) }
        runIt { resolve("${kdModule.name}-pretty.json").writeText(kdModule.encodeToJson(prettyPrint = true)) }
        runIt { resolve("${kdModule.name}.cbor").writeBytes(kdModule.encodeToCbor()) }
//            runIt { resolve("${kdModule.name}.schema").writeText(protoSchema()) }
//            runIt { resolve("${kdModule.name}.pb").writeBytes(kdModule.encodeToProtoBuf()) }
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
        //properties.mapNotNullTo(this) { it.toKdProperty(sourceSet) }
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
private fun DFunction.toKdFunction(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
): KdFunction? {
    if (!sourceSets.contains(sourceSet)) return null

    val extraModifiers = extraModifiers(sourceSet)
    val annotations = directAnnotations(sourceSet)
    val tagWrappers = tagWrappers(sourceSet) {
        it is Description || it is Return || it is Throws || it is Param || it is Sample // TODO: support samples
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
private fun DFunction.toKdConstructor(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
): KdConstructor? {
    if (!sourceSets.contains(sourceSet)) return null

    val extraModifiers = extraModifiers(sourceSet)
    val annotations = directAnnotations(sourceSet)
    val tagWrappers = tagWrappers(sourceSet) {
        it is Description || it is Return || it is Throws || it is Param || it is Sample // TODO: support samples
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
        it is Description || it is Param || it is Sample
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
            //properties.mapNotNullTo(this) { it.toKdProperty(sourceSet) }
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
    val tagWrappers = tagWrappers(sourceSet) { it is Description }

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

internal fun <T : Any> List<T>.singleOrNullIfEmpty(): T? = when (size) {
    0 -> null
    else -> single()
}

internal fun Documentable.extraModifiers(
    sourceSet: DokkaConfiguration.DokkaSourceSet
): Set<ExtraModifiers> {
    @Suppress("UNCHECKED_CAST")
    this as WithExtraProperties<out Documentable>
    return extra[AdditionalModifiers]?.content?.get(sourceSet).orEmpty()
}

// TODO: ignore file level annotations?
internal fun Documentable.directAnnotations(
    sourceSet: DokkaConfiguration.DokkaSourceSet
): List<Annotations.Annotation> {
    @Suppress("UNCHECKED_CAST")
    this as WithExtraProperties<out Documentable>
    return extra[Annotations]?.directAnnotations?.get(sourceSet).orEmpty()
}
