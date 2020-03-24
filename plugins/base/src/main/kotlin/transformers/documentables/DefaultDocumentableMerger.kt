package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DPackage
import org.jetbrains.dokka.model.DAnnotation
import org.jetbrains.dokka.model.properties.mergeExtras
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal object DefaultDocumentableMerger : DocumentableMerger {

    override fun invoke(modules: Collection<DModule>, context: DokkaContext): DModule {

        val projectName =
            modules.fold(modules.first().name) { acc, module -> acc.commonPrefixWith(module.name) }.takeIf { it.isNotEmpty() }
                ?: "project"

        return modules.reduce { left, right ->
            val list = listOf(left, right)
            DModule(
                name = projectName,
                packages = merge(
                    list.flatMap { it.packages },
                    DPackage::mergeWith
                ),
                documentation = list.platformDependentFor { documentation },
                platformData = list.flatMap { it.platformData }.distinct()
            ).mergeExtras(left, right)
        }
    }
}

private fun <T : Documentable> merge(elements: List<T>, reducer: (T, T) -> T): List<T> =
    elements.groupingBy { it.dri }
        .reduce { _, left, right -> reducer(left, right) }
        .values.toList()

private fun <T : Any, D : Documentable> Iterable<D>.platformDependentFor(
    selector: D.() -> PlatformDependent<T>
): PlatformDependent<T> {
    val actuals = map { it.selector().map }
        .flatMap { it.entries }
        .associate { (k, v) -> k to v }

    val expected = firstNotNullResult { it.selector().expect }

    return PlatformDependent(actuals, expected)
}

private fun <T : Any> PlatformDependent<T>.mergeWith(other: PlatformDependent<T>) = PlatformDependent(
    map = this + other,
    expect = expect ?: other.expect
)

private fun <T> mergeExpectActual(
    elements: List<T>,
    reducer: (T, T) -> T,
    platformSetter: T.(List<PlatformData>) -> T
): List<T> where T : Documentable, T : WithExpectActual {

    fun findExpect(actual: T, expects: List<T>): Expect<T> =
        expects.find { it.platformData.containsAll(actual.platformData) }.let { Expect.from(it) }

    fun reduceExpectActual(entry: Map.Entry<Expect<T>, List<T>>): List<T> = when (val expect = entry.key) {
        Expect.NotFound -> entry.value
        is Expect.Found -> entry.value.plus(expect.expect).reduce(reducer).let(::listOf)
    }

    fun analyzeExpectActual(sameDriElements: List<T>): List<T> {
        val pathGrouped: Collection<T> = mutableMapOf<Set<String>, T>().apply {
            sameDriElements.forEach { documentable ->
                val paths = documentable.sources.allValues.map { it.path }.toSet()
                val key = keys.find { it.containsAll(paths) }
                if (key == null) {
                    put(paths, documentable)
                } else {
                    computeIfPresent(key) { _, old -> reducer(old, documentable) }
                }
            }
        }.values
        val (expect, actual) = pathGrouped.partition { it.sources.expect != null }
        val mergedExpect = expect.groupBy { it.sources.expect?.path }.values.map { e ->
            e.first().platformSetter(e.flatMap { it.platformData }.distinct())
        }
        val groupExpectActual = actual.groupBy { findExpect(it, mergedExpect) }
        val pathsToExpects: Set<String> =
            groupExpectActual.keys.filterIsInstance<Expect.Found<T>>()
                .mapNotNull { it.expect.sources.expect?.path }.toSet()

        return groupExpectActual.flatMap { reduceExpectActual(it) } + expect.filterNot { it.sources.expect?.path in pathsToExpects }
    }

    return elements.groupBy { it.dri }.values.flatMap(::analyzeExpectActual)
}

private sealed class Expect<out T : Any> {
    object NotFound : Expect<Nothing>()
    data class Found<T : Any>(val expect: T) : Expect<T>()

    companion object {
        fun <T : Any> from(t: T?) = t?.let(::Found) ?: NotFound
    }
}

fun DPackage.mergeWith(other: DPackage): DPackage = copy(
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith, DClasslike::setPlatformData),
    documentation = documentation.mergeWith(other.documentation),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun DFunction.mergeWith(other: DFunction): DFunction = copy(
    parameters = merge(this.parameters + other.parameters, DParameter::mergeWith),
    receiver = receiver?.let { r -> other.receiver?.let { r.mergeWith(it) } ?: r } ?: other.receiver,
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    modifier = modifier.mergeWith(other.modifier),
    platformData = (platformData + other.platformData).distinct(),
    generics = merge(generics + other.generics, DTypeParameter::mergeWith)
).mergeExtras(this, other)

fun DProperty.mergeWith(other: DProperty): DProperty = copy(
    receiver = receiver?.let { r -> other.receiver?.let { r.mergeWith(it) } ?: r } ?: other.receiver,
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    modifier = modifier.mergeWith(other.modifier),
    platformData = (platformData + other.platformData).distinct(),
    getter = getter?.let { g -> other.getter?.let { g.mergeWith(it) } ?: g } ?: other.getter,
    setter = setter?.let { s -> other.setter?.let { s.mergeWith(it) } ?: s } ?: other.setter
).mergeExtras(this, other)

fun DClasslike.setPlatformData(platformData: List<PlatformData>): DClasslike = when (this) {
    is DClass -> copy(platformData = platformData)
    is DEnum -> copy(platformData = platformData)
    is DInterface -> copy(platformData = platformData)
    is DObject -> copy(platformData = platformData)
    else -> throw IllegalStateException("${this::class.qualifiedName} ${this.name} cannot have platform set")
}

fun DClasslike.mergeWith(other: DClasslike): DClasslike = when {
    this is DClass && other is DClass -> mergeWith(other)
    this is DEnum && other is DEnum -> mergeWith(other)
    this is DInterface && other is DInterface -> mergeWith(other)
    this is DObject && other is DObject -> mergeWith(other)
    this is DAnnotation && other is DAnnotation -> mergeWith(other)
    else -> throw IllegalStateException("${this::class.qualifiedName} ${this.name} cannot be mergesd with ${other::class.qualifiedName} ${other.name}")
}

fun DClass.mergeWith(other: DClass): DClass = copy(
    constructors = mergeExpectActual(
        constructors + other.constructors,
        DFunction::mergeWith
    ) { copy(platformData = it) },
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith, DClasslike::setPlatformData),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    generics = merge(generics + other.generics, DTypeParameter::mergeWith),
    modifier = modifier.mergeWith(other.modifier),
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun DEnum.mergeWith(other: DEnum): DEnum = copy(
    entries = merge(entries + other.entries, DEnumEntry::mergeWith),
    constructors = mergeExpectActual(
        constructors + other.constructors,
        DFunction::mergeWith
    ) { copy(platformData = it) },
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith, DClasslike::setPlatformData),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun DEnumEntry.mergeWith(other: DEnumEntry): DEnumEntry = copy(
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith, DClasslike::setPlatformData),
    documentation = documentation.mergeWith(other.documentation),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun DObject.mergeWith(other: DObject): DObject = copy(
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith, DClasslike::setPlatformData),
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun DInterface.mergeWith(other: DInterface): DInterface = copy(
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith, DClasslike::setPlatformData),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    generics = merge(generics + other.generics, DTypeParameter::mergeWith),
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun DAnnotation.mergeWith(other: DAnnotation): DAnnotation = copy(
    constructors = mergeExpectActual(
        constructors + other.constructors,
        DFunction::mergeWith
    ) { copy(platformData = it) },
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith, DClasslike::setPlatformData),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun DParameter.mergeWith(other: DParameter): DParameter = copy(
    documentation = documentation.mergeWith(other.documentation),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun DTypeParameter.mergeWith(other: DTypeParameter): DTypeParameter = copy(
    documentation = documentation.mergeWith(other.documentation),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)