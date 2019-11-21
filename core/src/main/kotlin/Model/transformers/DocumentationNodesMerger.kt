package org.jetbrains.dokka.Model.transformers

import org.jetbrains.dokka.Model.*
import org.jetbrains.dokka.Model.Function

internal object DocumentationNodesMerger : DocumentationNodeTransformer {
    override fun invoke(original: Module) = Module(
        original.packages.map { mergePackageContent(it) }
    )
    override fun invoke(modules: Collection<Module>): Module =
        Module(merge(modules.flatMap { it.packages }, Package::mergeWith))
}

private fun mergePackageContent(original: Package) = Package(
    original.dri,
    merge(original.functions, Function::mergeWith),
    merge(original.properties, Property::mergeWith),
    merge(original.classes, Class::mergeWith)
)

private fun <T: DocumentationNode> merge(elements: List<T>, reducer: (T, T) -> T): List<T> =
    elements.groupingBy { it.dri }
        .reduce { _, left, right -> reducer(left, right)}
        .values.toList()

fun PlatformInfo.mergeWith(other: PlatformInfo?) = BasePlatformInfo(
    docTag,
    links,
    (platformData + (other?.platformData ?: emptyList())).distinct()
)

fun ClassPlatformInfo.mergeWith(other: ClassPlatformInfo?) = ClassPlatformInfo(
    info.mergeWith(other?.info),
    (inherited + (other?.inherited ?: emptyList())).distinct()
)

fun List<ClassPlatformInfo>.mergeClassPlatformInfo() : List<ClassPlatformInfo> =
    groupingBy { it.docTag.toString() + it.links + it.inherited}.reduce {
            _, left, right -> left.mergeWith(right)
    }.values.toList()

fun List<PlatformInfo>.merge() : List<PlatformInfo> =
    groupingBy { it.docTag.toString() + it.links }.reduce {
        _, left, right -> left.mergeWith(right)
    }.values.toList()

fun Function.mergeWith(other: Function) = Function(
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

fun Class.mergeWith(other: Class) = Class(
    dri,
    name,
    kind,
    merge(constructors + other.constructors, Function::mergeWith),
    merge(functions + other.functions, Function::mergeWith),
    merge(properties + other.properties, Property::mergeWith),
    merge(classes + other.classes, Class::mergeWith),
    expected?.mergeWith(other.expected),
    (actual + other.actual).mergeClassPlatformInfo()
)

fun Parameter.mergeWith(other: Parameter) = Parameter(
    dri,
    name,
    type,
    (actual + other.actual).merge()
)

fun Package.mergeWith(other: Package) = Package(
    dri,
    merge(functions + other.functions, Function::mergeWith),
    merge(properties + other.properties, Property::mergeWith),
    merge(classes + other.classes, Class::mergeWith)
)