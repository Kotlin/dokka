package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.Package
import org.jetbrains.dokka.model.Annotation
import org.jetbrains.dokka.model.properties.mergeExtras
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal object DefaultDocumentableMerger : DocumentableMerger {
    override fun invoke(modules: Collection<Module>, context: DokkaContext): Module {
        val name = modules.map { it.name }.distinct().singleOrNull() ?: run {
            context.logger.error("All module names need to be the same")
            modules.first().name
        }

        return modules.reduce { left, right ->
            val list = listOf(left, right)

            Module(
                name = name,
                packages = merge(
                    list.flatMap { it.packages },
                    Package::mergeWith
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
        val (expect, actual) = sameDriElements.partition { it.sources.expect != null }
        val mergedExpect = expect.groupBy { it.sources.expect?.path }.values.map { e ->
            e.first().platformSetter(e.flatMap { it.platformData }.distinct())
        }
        val groupExpectActual = actual.groupBy { findExpect(it, mergedExpect) }
        val pathsToExpects: Set<String> = groupExpectActual.keys.filterIsInstance<Expect.Found<T>>().mapNotNull { it.expect.sources.expect?.path }.toSet()

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

fun Package.mergeWith(other: Package): Package = copy(
    functions = mergeExpectActual(functions + other.functions, Function::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, Property::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, Classlike::mergeWith, Classlike::setPlatformData),
    packages = merge(packages + other.packages, Package::mergeWith),
    documentation = documentation.mergeWith(other.documentation),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun Function.mergeWith(other: Function): Function = copy(
    parameters = merge(this.parameters + other.parameters, Parameter::mergeWith),
    receiver = receiver?.let { r -> other.receiver?.let { r.mergeWith(it) } ?: r } ?: other.receiver,
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct(),
    generics = merge(generics + other.generics, TypeParameter::mergeWith)
).mergeExtras(this, other)

fun Property.mergeWith(other: Property): Property = copy(
    receiver = receiver?.let { r -> other.receiver?.let { r.mergeWith(it) } ?: r } ?: other.receiver,
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct(),
    getter = getter?.let { g -> other.getter?.let { g.mergeWith(it) } ?: g } ?: other.getter,
    setter = setter?.let { s -> other.setter?.let { s.mergeWith(it) } ?: s } ?: other.setter
).mergeExtras(this, other)

fun Classlike.setPlatformData(platformData: List<PlatformData>): Classlike = when (this) {
    is Class -> copy(platformData = platformData)
    is Enum -> copy(platformData = platformData)
    is Interface -> copy(platformData = platformData)
    is Object -> copy(platformData = platformData)
    else -> throw IllegalStateException("${this::class.qualifiedName} ${this.name} cannot have platform set")
}

fun Classlike.mergeWith(other: Classlike): Classlike = when {
    this is Class && other is Class -> mergeWith(other)
    this is Enum && other is Enum -> mergeWith(other)
    this is Interface && other is Interface -> mergeWith(other)
    this is Object && other is Object -> mergeWith(other)
    this is Annotation && other is Annotation -> mergeWith(other)
    else -> throw IllegalStateException("${this::class.qualifiedName} ${this.name} cannot be mergesd with ${other::class.qualifiedName} ${other.name}")
}

fun Class.mergeWith(other: Class): Class = copy(
    constructors = mergeExpectActual(
        constructors + other.constructors,
        Function::mergeWith
    ) { copy(platformData = it) },
    functions = mergeExpectActual(functions + other.functions, Function::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, Property::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, Classlike::mergeWith, Classlike::setPlatformData),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    generics = merge(generics + other.generics, TypeParameter::mergeWith),
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun Enum.mergeWith(other: Enum): Enum = copy(
    entries = merge(entries + other.entries, EnumEntry::mergeWith),
    constructors = mergeExpectActual(
        constructors + other.constructors,
        Function::mergeWith
    ) { copy(platformData = it) },
    functions = mergeExpectActual(functions + other.functions, Function::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, Property::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, Classlike::mergeWith, Classlike::setPlatformData),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun EnumEntry.mergeWith(other: EnumEntry): EnumEntry = copy(
    functions = mergeExpectActual(functions + other.functions, Function::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, Property::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, Classlike::mergeWith, Classlike::setPlatformData),
    documentation = documentation.mergeWith(other.documentation),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun Object.mergeWith(other: Object): Object = copy(
    functions = mergeExpectActual(functions + other.functions, Function::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, Property::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, Classlike::mergeWith, Classlike::setPlatformData),
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun Interface.mergeWith(other: Interface): Interface = copy(
    functions = mergeExpectActual(functions + other.functions, Function::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, Property::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, Classlike::mergeWith, Classlike::setPlatformData),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    generics = merge(generics + other.generics, TypeParameter::mergeWith),
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun Annotation.mergeWith(other: Annotation): Annotation = copy(
    constructors = mergeExpectActual(
        constructors + other.constructors,
        Function::mergeWith
    ) { copy(platformData = it) },
    functions = mergeExpectActual(functions + other.functions, Function::mergeWith) { copy(platformData = it) },
    properties = mergeExpectActual(properties + other.properties, Property::mergeWith) { copy(platformData = it) },
    classlikes = mergeExpectActual(classlikes + other.classlikes, Classlike::mergeWith, Classlike::setPlatformData),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun Parameter.mergeWith(other: Parameter): Parameter = copy(
    documentation = documentation.mergeWith(other.documentation),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)

fun TypeParameter.mergeWith(other: TypeParameter): TypeParameter = copy(
    documentation = documentation.mergeWith(other.documentation),
    platformData = (platformData + other.platformData).distinct()
).mergeExtras(this, other)