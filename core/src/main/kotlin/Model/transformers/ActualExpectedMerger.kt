package org.jetbrains.dokka.Model.transformers

import org.jetbrains.dokka.Model.*
import org.jetbrains.dokka.Model.Function

internal object ActualExpectedMerger : DocumentationNodeTransformer {
    override fun invoke(original: Module) = Module(
        original.packages.map { mergePackageContent(it) }
    )
}

private fun mergePackageContent(original: Package) = Package(
    original.dri,
    merge(original.functions, Function::mergeWith),
    merge(original.properties, Property::mergeWith),
    merge(original.classes, Class::mergeWith)
)

private fun <T: DocumentationNode<*>> merge(elements: List<T>, reducer: (T, T) -> T): List<T> =
    elements.groupingBy { it.dri }
        .reduce { _, left, right -> reducer(left, right)}
        .values.toList()

fun Function.mergeWith(other: Function) = Function(
    dri,
    name,
    if (receiver != null && other.receiver != null) receiver.mergeWith(other.receiver) else null,
    merge(parameters + other.parameters, Parameter::mergeWith),
    docTags + other.docTags,
    descriptors + other.descriptors
)

fun Property.mergeWith(other: Property) = Property(
    dri,
    name,
    if (receiver != null && other.receiver != null) receiver.mergeWith(other.receiver) else null,
    docTags + other.docTags,
    descriptors + other.descriptors
)

fun Class.mergeWith(other: Class) = Class(
    dri,
    name,
    merge(constructors + other.constructors, Function::mergeWith),
    merge(functions + other.functions, Function::mergeWith),
    merge(properties + other.properties, Property::mergeWith),
    merge(classes + other.classes, Class::mergeWith),
    docTags + other.docTags,
    descriptors + other.descriptors
)

fun Parameter.mergeWith(other: Parameter) = Parameter(
    dri,
    name,
    docTags + other.docTags,
    descriptors + other.descriptors
)
