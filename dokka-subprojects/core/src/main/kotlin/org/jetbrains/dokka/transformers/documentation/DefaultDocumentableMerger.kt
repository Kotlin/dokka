/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy
import org.jetbrains.dokka.model.properties.mergeExtras
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.ExperimentalDokkaApi

/**
 * Should NOT be used outside of Dokka itself, there are no guarantees
 * this class will continue to exist in future releases.
 *
 * This class resides in core because it is a non-trivial implementation
 * for a core extension [CoreExtensions.documentableMerger], which is needed
 * in modules that only have access to `dokka-core`.
 */
@InternalDokkaApi
public class DefaultDocumentableMerger(context: DokkaContext) : DocumentableMerger {
    private val dependencyInfo = context.getDependencyInfo()

    override fun invoke(modules: Collection<DModule>): DModule? =
        modules.reduceOrNull { left, right ->
            val list = listOf(left, right)
            DModule(
                name = modules.map { it.name }.distinct().joinToString("|"),
                packages = merge(
                    list.flatMap { it.packages }
                ) { pck1, pck2 -> pck1.mergeWith(pck2) },
                documentation = list.map { it.documentation }.flatMap { it.entries }.associate { (k, v) -> k to v },
                expectPresentInSet = list.firstNotNullOfOrNull { it.expectPresentInSet },
                sourceSets = list.flatMap { it.sourceSets }.toSet()
            ).mergeExtras(left, right)
        }

    private fun DokkaContext.getDependencyInfo()
            : Map<DokkaConfiguration.DokkaSourceSet, List<DokkaConfiguration.DokkaSourceSet>> {

        fun getDependencies(sourceSet: DokkaConfiguration.DokkaSourceSet): List<DokkaConfiguration.DokkaSourceSet> =
            listOf(sourceSet) + configuration.sourceSets.filter {
                it.sourceSetID in sourceSet.dependentSourceSets
            }.flatMap { getDependencies(it) }

        return configuration.sourceSets.associateWith { getDependencies(it) }
    }

    private fun <T : Documentable> merge(elements: List<T>, reducer: (T, T) -> T): List<T> =
        elements.groupingBy { it.dri }
            .reduce { _, left, right -> reducer(left, right) }
            .values.toList()

    private fun <T> mergeExpectActual(
        elements: List<T>,
        reducer: (T, T) -> T
    ): List<T> where T : Documentable, T : WithSources {

        fun mergeClashingElements(elements: List<Pair<T, Set<DokkaConfiguration.DokkaSourceSet>>>): List<T> =
            elements.groupBy { it.first.name }.values.flatMap { listOfDocumentableToSSIds ->
                val merged = listOfDocumentableToSSIds.map { (documentable, sourceSets) ->
                    when (documentable) {
                        is DClass -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DObject -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DAnnotation -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DInterface -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DEnum -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DFunction -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        is DProperty -> documentable.copy(
                            extra = documentable.extra + ClashingDriIdentifier(
                                sourceSets + (documentable.extra[ClashingDriIdentifier]?.value ?: emptySet())
                            )
                        )
                        else -> documentable
                    }
                }
                @Suppress("UNCHECKED_CAST")
                merged as List<T>
            }


        fun analyzeExpectActual(sameDriElements: List<T>): List<T> {
            val (expects, actuals) = sameDriElements.partition { it.expectPresentInSet != null }
            // It's possible that there are no `expect` declarations, but there are `actual` declarations,
            // e.g. in case `expect` is `internal` or filtered previously for some other reason.
            // In this case we just merge `actual` declarations without `expect`
            val groupedActualsWithSourceSets = if (expects.isEmpty()) {
                listOf(actuals to actuals.flatMap { it.sourceSets }.toSet())
            } else expects.map { expect ->
                val actualsForGivenExpect = actuals.filter { actual ->
                    // [actual.sourceSets] can already be partially merged and contain more than one source set
                    actual.sourceSets.all {
                        dependencyInfo[it]
                        ?.contains(expect.expectPresentInSet!!)
                        ?: throw IllegalStateException("Cannot resolve expect/actual relation for ${actual.name}")
                    }
                }
                (listOf(expect) + actualsForGivenExpect) to actualsForGivenExpect.flatMap { it.sourceSets }.toSet()
            }
            val reducedToOneDocumentableWithActualSourceSetIds =
                groupedActualsWithSourceSets.map { it.first.reduce(reducer) to it.second }
            return reducedToOneDocumentableWithActualSourceSetIds.let(::mergeClashingElements)
        }


        return elements.partition {
            (it as? WithIsExpectActual)?.isExpectActual ?: false
        }.let { (expectActuals, notExpectActuals) ->
            notExpectActuals.map { it to it.sourceSets }
                .groupBy { it.first.dri }.values.flatMap(::mergeClashingElements) +
                    expectActuals.groupBy { it.dri }.values.flatMap(::analyzeExpectActual)
        }
    }

    public fun DPackage.mergeWith(other: DPackage): DPackage = copy(
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        typealiases = merge(typealiases + other.typealiases) { ta1, ta2 -> ta1.mergeWith(ta2) },
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    public fun DFunction.mergeWith(other: DFunction): DFunction = copy(
        parameters = merge(this.parameters + other.parameters) { p1, p2 -> p1.mergeWith(p2) },
        receiver = receiver?.let { r -> other.receiver?.let { r.mergeWith(it) } ?: r } ?: other.receiver,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        modifier = modifier + other.modifier,
        sourceSets = sourceSets + other.sourceSets,
        generics = merge(generics + other.generics) { tp1, tp2 -> tp1.mergeWith(tp2) },
        contextParameters = @OptIn(ExperimentalDokkaApi::class) merge(this.contextParameters + other.contextParameters) { p1, p2 -> p1.mergeWith(p2) }
    ).mergeExtras(this, other)

    public fun DProperty.mergeWith(other: DProperty): DProperty = copy(
        receiver = receiver?.let { r -> other.receiver?.let { r.mergeWith(it) } ?: r } ?: other.receiver,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        modifier = modifier + other.modifier,
        sourceSets = sourceSets + other.sourceSets,
        getter = getter?.let { g -> other.getter?.let { g.mergeWith(it) } ?: g } ?: other.getter,
        setter = setter?.let { s -> other.setter?.let { s.mergeWith(it) } ?: s } ?: other.setter,
        generics = merge(generics + other.generics) { tp1, tp2 -> tp1.mergeWith(tp2) },
        contextParameters = @OptIn(ExperimentalDokkaApi::class) merge(this.contextParameters + other.contextParameters) { p1, p2 -> p1.mergeWith(p2) }
    ).mergeExtras(this, other)

    private fun DClasslike.mergeWith(other: DClasslike): DClasslike = when {
        this is DClass && other is DClass -> mergeWith(other)
        this is DEnum && other is DEnum -> mergeWith(other)
        this is DInterface && other is DInterface -> mergeWith(other)
        this is DObject && other is DObject -> mergeWith(other)
        this is DAnnotation && other is DAnnotation -> mergeWith(other)
        else -> throw IllegalStateException("${this::class.qualifiedName} ${this.name} cannot be merged with ${other::class.qualifiedName} ${other.name}")
    }

    private fun DClass.mergeWith(other: DClass): DClass = copy(
        constructors = mergeExpectActual(
            constructors + other.constructors
        ) { f1, f2 -> f1.mergeWith(f2) },
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
        generics = merge(generics + other.generics) { tp1, tp2 -> tp1.mergeWith(tp2) },
        modifier = modifier + other.modifier,
        supertypes = supertypes + other.supertypes,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    private fun DEnum.mergeWith(other: DEnum): DEnum = copy(
        entries = merge(entries + other.entries) { ee1, ee2 -> ee1.mergeWith(ee2) },
        constructors = mergeExpectActual(
            constructors + other.constructors
        ) { f1, f2 -> f1.mergeWith(f2) },
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
        supertypes = supertypes + other.supertypes,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    private fun DEnumEntry.mergeWith(other: DEnumEntry): DEnumEntry = copy(
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    private fun DObject.mergeWith(other: DObject): DObject = copy(
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        supertypes = supertypes + other.supertypes,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    private fun DInterface.mergeWith(other: DInterface): DInterface = copy(
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
        generics = merge(generics + other.generics) { tp1, tp2 -> tp1.mergeWith(tp2) },
        supertypes = supertypes + other.supertypes,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    private fun DAnnotation.mergeWith(other: DAnnotation): DAnnotation = copy(
        constructors = mergeExpectActual(
            constructors + other.constructors
        ) { f1, f2 -> f1.mergeWith(f2) },
        functions = mergeExpectActual(functions + other.functions) { f1, f2 -> f1.mergeWith(f2) },
        properties = mergeExpectActual(properties + other.properties) { p1, p2 -> p1.mergeWith(p2) },
        classlikes = mergeExpectActual(classlikes + other.classlikes) { c1, c2 -> c1.mergeWith(c2) },
        companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sources = sources + other.sources,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets,
        generics = merge(generics + other.generics) { tp1, tp2 -> tp1.mergeWith(tp2) }
    ).mergeExtras(this, other)

    private fun DParameter.mergeWith(other: DParameter): DParameter = copy(
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    private fun DTypeParameter.mergeWith(other: DTypeParameter): DTypeParameter = copy(
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)

    private fun DTypeAlias.mergeWith(other: DTypeAlias): DTypeAlias = copy(
        documentation = documentation + other.documentation,
        expectPresentInSet = expectPresentInSet ?: other.expectPresentInSet,
        underlyingType = underlyingType + other.underlyingType,
        visibility = visibility + other.visibility,
        sourceSets = sourceSets + other.sourceSets
    ).mergeExtras(this, other)
}

public data class ClashingDriIdentifier(val value: Set<DokkaConfiguration.DokkaSourceSet>) : ExtraProperty<Documentable> {
    public companion object : ExtraProperty.Key<Documentable, ClashingDriIdentifier> {
        override fun mergeStrategyFor(
            left: ClashingDriIdentifier,
            right: ClashingDriIdentifier
        ): MergeStrategy<Documentable> =
            MergeStrategy.Replace(ClashingDriIdentifier(left.value + right.value))
    }

    override val key: ExtraProperty.Key<Documentable, *> = ClashingDriIdentifier
}

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
private inline fun <T, R : Any> Iterable<T>.firstNotNullOfOrNull(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) {
            return result
        }
    }
    return null
}
