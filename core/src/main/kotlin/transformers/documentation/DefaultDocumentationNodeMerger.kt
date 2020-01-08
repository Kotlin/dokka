package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaConsoleLogger

internal object DefaultDocumentationNodeMerger : DocumentationNodeMerger {
    override fun invoke(modules: Collection<Module>, context: DokkaContext): Module {
        if (!modules.all { it.name == modules.first().name })
            DokkaConsoleLogger.error("All module names need to be the same")
        return Module(
            modules.first().name,
            merge(
                modules.flatMap { it.packages },
                Package::mergeWith
            )
        )
    }
}

private fun <T : Documentable> merge(elements: List<T>, reducer: (T, T) -> T): List<T> =
    elements.groupingBy { it.dri }
        .reduce { _, left, right -> reducer(left, right) }
        .values.toList()

fun PlatformInfo.mergeWith(other: PlatformInfo?) = BasePlatformInfo(
    documentationNode,
    (platformData + (other?.platformData ?: emptyList())).distinct()
)

fun ClassPlatformInfo.mergeWith(other: ClassPlatformInfo?) = ClassPlatformInfo(
    info.mergeWith(other?.info),
    (inherited + (other?.inherited ?: emptyList())).distinct()
)

fun List<ClassPlatformInfo>.mergeClassPlatformInfo(): List<ClassPlatformInfo> =
    groupingBy { it.documentationNode.children + it.inherited }.reduce { _, left, right ->
        left.mergeWith(right)
    }.values.toList()

fun List<PlatformInfo>.merge(): List<PlatformInfo> =
    groupingBy { it.documentationNode }.reduce { _, left, right ->
        left.mergeWith(right)
    }.values.toList()

fun Function.mergeWith(other: Function): Function = Function(
    dri,
    name,
    returnType,
    isConstructor,
    if (receiver != null && other.receiver != null) receiver.mergeWith(other.receiver) else null,
    merge(parameters + other.parameters, Parameter::mergeWith),
    expected?.mergeWith(other.expected),
    (actual + other.actual).merge()
)

fun Property.mergeWith(other: Property) = Property(
    dri,
    name,
    if (receiver != null && other.receiver != null) receiver.mergeWith(other.receiver) else null,
    expected?.mergeWith(other.expected),
    (actual + other.actual).merge()
)

fun Classlike.mergeWith(other: Classlike): Classlike = when {
    this is Class && other is Class -> mergeWith(other)
    this is Enum && other is Enum -> mergeWith(other)
    else -> throw IllegalStateException("${this::class.qualifiedName} ${this.name} cannot be merged with ${other::class.qualifiedName} ${other.name}")
}

fun Class.mergeWith(other: Class) = Class(
    dri = dri,
    name = name,
    kind = kind,
    constructors = merge(constructors + other.constructors, Function::mergeWith),
    functions = merge(functions + other.functions, Function::mergeWith),
    properties = merge(properties + other.properties, Property::mergeWith),
    classlikes = merge(classlikes + other.classlikes, Classlike::mergeWith),
    expected = expected?.mergeWith(other.expected),
    actual = (actual + other.actual).mergeClassPlatformInfo()
)

fun Enum.mergeWith(other: Enum) = Enum(
    dri = dri,
    name = name,
    functions = merge(functions + other.functions, Function::mergeWith),
    properties = merge(properties + other.properties, Property::mergeWith),
    classlikes = merge(classlikes + other.classlikes, Classlike::mergeWith),
    expected = expected?.mergeWith(other.expected),
    actual = (actual + other.actual).mergeClassPlatformInfo(),
    entries = (this.entries + other.entries.distinctBy { it.dri }.toList()),
    constructors = merge(constructors + other.constructors, Function::mergeWith)
)

fun Parameter.mergeWith(other: Parameter) = Parameter(
    dri,
    name,
    type,
    (actual + other.actual).merge()
)

fun Package.mergeWith(other: Package): Package = Package(
    dri,
    merge(functions + other.functions, Function::mergeWith),
    merge(properties + other.properties, Property::mergeWith),
    merge(classlikes + other.classlikes, Classlike::mergeWith)
)