package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.mergeExtras
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal object DefaultDocumentableMerger : DocumentableMerger {

    override fun invoke(modules: Collection<DModule>, context: DokkaContext): DModule {

        val projectName =
            modules.fold(modules.first().name) { acc, module -> acc.commonPrefixWith(module.name) }
                .takeIf { it.isNotEmpty() }
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
                sourceSets = list.flatMap { it.sourceSets }.distinct()
            ).mergeExtras(left, right)
        }
    }
}

private fun <T : Documentable> merge(elements: List<T>, reducer: (T, T) -> T): List<T> =
    elements.groupingBy { it.dri }
        .reduce { _, left, right -> reducer(left, right) }
        .values.toList()

private fun <T : Any, D : Documentable> Iterable<D>.platformDependentFor(
    selector: D.() -> SourceSetDependent<T>
): SourceSetDependent<T> {
    val actuals = map { it.selector().map }
        .flatMap { it.entries }
        .associate { (k, v) -> k to v }

    val expected = firstNotNullResult { it.selector().expect }

    return SourceSetDependent(actuals, expected)
}

private fun <T : Any> SourceSetDependent<T>.mergeWith(other: SourceSetDependent<T>) = SourceSetDependent(
    map = this + other,
    expect = expect ?: other.expect
)

private fun <T> mergeExpectActual(
    elements: List<T>,
    reducer: (T, T) -> T
): List<T> where T : Documentable, T : WithExpectActual {

    fun analyzeExpectActual(sameDriElements: List<T>) = sameDriElements.reduce(reducer)

    return elements.groupBy { it.dri }.values.map(::analyzeExpectActual)
}

private sealed class Expect<out T : Any> {
    object NotFound : Expect<Nothing>()
    data class Found<T : Any>(val expect: T) : Expect<T>()

    companion object {
        fun <T : Any> from(t: T?) = t?.let(::Found) ?: NotFound
    }
}

fun DPackage.mergeWith(other: DPackage): DPackage = copy(
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith),
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith),
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith),
    documentation = documentation.mergeWith(other.documentation),
    typealiases = merge(typealiases + other.typealiases, DTypeAlias::mergeWith),
    sourceSets = (sourceSets + other.sourceSets).distinct()
).mergeExtras(this, other)

fun DFunction.mergeWith(other: DFunction): DFunction = copy(
    parameters = merge(this.parameters + other.parameters, DParameter::mergeWith),
    receiver = receiver?.let { r -> other.receiver?.let { r.mergeWith(it) } ?: r } ?: other.receiver,
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    modifier = modifier.mergeWith(other.modifier),
    sourceSets = (sourceSets + other.sourceSets).distinct(),
    generics = merge(generics + other.generics, DTypeParameter::mergeWith)
).mergeExtras(this, other)

fun DProperty.mergeWith(other: DProperty): DProperty = copy(
    receiver = receiver?.let { r -> other.receiver?.let { r.mergeWith(it) } ?: r } ?: other.receiver,
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    modifier = modifier.mergeWith(other.modifier),
    sourceSets = (sourceSets + other.sourceSets).distinct(),
    getter = getter?.let { g -> other.getter?.let { g.mergeWith(it) } ?: g } ?: other.getter,
    setter = setter?.let { s -> other.setter?.let { s.mergeWith(it) } ?: s } ?: other.setter,
    generics = merge(generics + other.generics, DTypeParameter::mergeWith)
).mergeExtras(this, other)

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
    ),
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith),
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith),
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    generics = merge(generics + other.generics, DTypeParameter::mergeWith),
    modifier = modifier.mergeWith(other.modifier),
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    sourceSets = (sourceSets + other.sourceSets).distinct()
).mergeExtras(this, other)

fun DEnum.mergeWith(other: DEnum): DEnum = copy(
    entries = merge(entries + other.entries, DEnumEntry::mergeWith),
    constructors = mergeExpectActual(
        constructors + other.constructors,
        DFunction::mergeWith
    ),
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith),
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith),
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    sourceSets = (sourceSets + other.sourceSets).distinct()
).mergeExtras(this, other)

fun DEnumEntry.mergeWith(other: DEnumEntry): DEnumEntry = copy(
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith),
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith),
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith),
    documentation = documentation.mergeWith(other.documentation),
    sourceSets = (sourceSets + other.sourceSets).distinct()
).mergeExtras(this, other)

fun DObject.mergeWith(other: DObject): DObject = copy(
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith),
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith),
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith),
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    sourceSets = (sourceSets + other.sourceSets).distinct()
).mergeExtras(this, other)

fun DInterface.mergeWith(other: DInterface): DInterface = copy(
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith),
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith),
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    generics = merge(generics + other.generics, DTypeParameter::mergeWith),
    supertypes = supertypes.mergeWith(other.supertypes),
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    sourceSets = (sourceSets + other.sourceSets).distinct()
).mergeExtras(this, other)

fun DAnnotation.mergeWith(other: DAnnotation): DAnnotation = copy(
    constructors = mergeExpectActual(
        constructors + other.constructors,
        DFunction::mergeWith
    ),
    functions = mergeExpectActual(functions + other.functions, DFunction::mergeWith),
    properties = mergeExpectActual(properties + other.properties, DProperty::mergeWith),
    classlikes = mergeExpectActual(classlikes + other.classlikes, DClasslike::mergeWith),
    companion = companion?.let { c -> other.companion?.let { c.mergeWith(it) } ?: c } ?: other.companion,
    documentation = documentation.mergeWith(other.documentation),
    sources = sources.mergeWith(other.sources),
    visibility = visibility.mergeWith(other.visibility),
    sourceSets = (sourceSets + other.sourceSets).distinct()
).mergeExtras(this, other)

fun DParameter.mergeWith(other: DParameter): DParameter = copy(
    documentation = documentation.mergeWith(other.documentation),
    sourceSets = (sourceSets + other.sourceSets).distinct()
).mergeExtras(this, other)

fun DTypeParameter.mergeWith(other: DTypeParameter): DTypeParameter = copy(
    documentation = documentation.mergeWith(other.documentation),
    sourceSets = (sourceSets + other.sourceSets).distinct()
).mergeExtras(this, other)

fun DTypeAlias.mergeWith(other: DTypeAlias): DTypeAlias = copy(
    documentation = documentation.mergeWith(other.documentation),
    underlyingType = underlyingType.mergeWith(other.underlyingType),
    visibility = visibility.mergeWith(other.visibility),
    sourceSets = (sourceSets + other.sourceSets).distinct()
).mergeExtras(this, other)